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

package org.apache.james.jmap.memory.vacation;

import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.apache.james.jmap.api.vacation.AccountId;
import org.apache.james.jmap.api.vacation.NotificationRegistry;
import org.apache.james.jmap.api.vacation.RecipientId;
import org.apache.james.util.date.ZonedDateTimeProvider;

import com.google.common.base.Preconditions;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;

public class MemoryNotificationRegistry implements NotificationRegistry {

    public static class Entry {
        private final RecipientId recipientId;
        private final Optional<ZonedDateTime> expiryDate;

        public Entry(RecipientId recipientId, Optional<ZonedDateTime> expiryDate) {
            this.recipientId = recipientId;
            this.expiryDate = expiryDate;
        }

        public RecipientId getRecipientId() {
            return recipientId;
        }

        public Optional<ZonedDateTime> getExpiryDate() {
            return expiryDate;
        }
    }

    private final ZonedDateTimeProvider zonedDateTimeProvider;
    private final Multimap<AccountId, Entry> registrations;

    public MemoryNotificationRegistry(ZonedDateTimeProvider zonedDateTimeProvider) {
        this.zonedDateTimeProvider = zonedDateTimeProvider;
        this.registrations = Multimaps.synchronizedMultimap(HashMultimap.create());
}

    @Override
    public CompletableFuture<Void> register(AccountId accountId, RecipientId recipientId, Optional<ZonedDateTime> expiryDate) {
        expiryDate.ifPresent(zonedDateTime -> Preconditions.checkArgument(zonedDateTime.isAfter(zonedDateTimeProvider.get())));
        registrations.put(accountId, new Entry(recipientId, expiryDate));
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Boolean> isRegistered(AccountId accountId, RecipientId recipientId) {
        return CompletableFuture.completedFuture(
            registrations.get(accountId)
                .stream()
                .filter(entry -> entry.getRecipientId().equals(recipientId))
                .map(Entry::getExpiryDate)
                .findAny()
                .filter(optional -> optional.map(expiry -> zonedDateTimeProvider.get().isBefore(expiry)).orElse(true))
                .isPresent());
    }

    @Override
    public CompletableFuture<Void> flush(AccountId accountId) {
        registrations.removeAll(accountId);
        return CompletableFuture.completedFuture(null);
    }
}
