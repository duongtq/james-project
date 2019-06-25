/****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 *                                                              *
 *   http://www.apache.org/licenses/LICENSE-2.0                 *
 *                                                              *
 * Unless required by applicable law or agreed to in writing,   *
 * software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 * KIND, either express or implied.  See the License for the    *
 * specific language governing permissions and limitations      *
 * under the License.                                           *
 ****************************************************************/

package org.apache.james.vault;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.apache.james.blob.api.BucketName;
import org.apache.james.core.User;
import org.apache.james.mailbox.model.MessageId;
import org.reactivestreams.Publisher;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Table;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class MemoryDeletedMessageMetadataVault implements DeletedMessageMetadataVault {
    private final Table<BucketName, User, Map<MessageId, DeletedMessageWithStorageInformation>> table;

    public MemoryDeletedMessageMetadataVault() {
        table = HashBasedTable.create();
    }

    @Override
    public synchronized Publisher<Void> store(DeletedMessageWithStorageInformation deletedMessage) {
        BucketName bucketName = deletedMessage.getStorageInformation().getBucketName();
        User owner = deletedMessage.getDeletedmessage().getOwner();
        MessageId messageId = deletedMessage.getDeletedmessage().getMessageId();

        Map<MessageId, DeletedMessageWithStorageInformation> userVault = userVault(bucketName, owner);
        userVault.put(messageId, deletedMessage);
        table.put(bucketName, owner, userVault);

        return Mono.empty();
    }

    @Override
    public synchronized Publisher<Void> removeBucket(BucketName bucketName) {
        table.row(bucketName).clear();

        return Mono.empty();
    }

    @Override
    public synchronized Publisher<Void> remove(BucketName bucketName, User user, MessageId messageId) {
        userVault(bucketName, user).remove(messageId);

        return Mono.empty();
    }

    @Override
    public synchronized Publisher<StorageInformation> retrieveStorageInformation(User user, MessageId messageId) {
        return Flux.from(listBuckets())
            .flatMap(bucket -> Mono.justOrEmpty(userVault(bucket, user).get(messageId)))
            .map(DeletedMessageWithStorageInformation::getStorageInformation)
            .next();
    }

    @Override
    public synchronized Publisher<DeletedMessageWithStorageInformation> listMessages(BucketName bucketName, User user) {
        return Flux.fromIterable(Optional.ofNullable(table.get(bucketName, user))
            .map(Map::values)
            .orElse(ImmutableList.of()));
    }

    @Override
    public synchronized Publisher<BucketName> listBuckets() {
        return Flux.fromIterable(table.rowKeySet());
    }

    private Map<MessageId, DeletedMessageWithStorageInformation> userVault(BucketName bucketName, User owner) {
        return Optional.ofNullable(table.get(bucketName, owner))
            .orElse(new HashMap<>());
    }
}
