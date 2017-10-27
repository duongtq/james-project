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
package org.apache.james.mailbox.elasticsearch.events;

import static org.elasticsearch.index.query.QueryBuilders.termQuery;

import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.james.backends.es.ElasticSearchIndexer;
import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.MessageUid;
import org.apache.james.mailbox.elasticsearch.json.JsonMessageConstants;
import org.apache.james.mailbox.elasticsearch.json.MessageToElasticSearchJson;
import org.apache.james.mailbox.exception.MailboxException;
import org.apache.james.mailbox.model.UpdatedFlags;
import org.apache.james.mailbox.store.mail.model.Mailbox;
import org.apache.james.mailbox.store.mail.model.MailboxMessage;
import org.apache.james.mailbox.store.search.MessageIndexer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.ImmutableList;

public class ElasticSearchMessageIndexer implements MessageIndexer {

    private final static Logger LOGGER = LoggerFactory.getLogger(ElasticSearchMessageIndexer.class);
    private final static String ID_SEPARATOR = ":";

    private final ElasticSearchIndexer indexer;
    private final MessageToElasticSearchJson messageToElasticSearchJson;

    @Inject
    public ElasticSearchMessageIndexer(ElasticSearchIndexer indexer,
                                       MessageToElasticSearchJson messageToElasticSearchJson) {
        this.indexer = indexer;
        this.messageToElasticSearchJson = messageToElasticSearchJson;
    }

    @Override
    public void add(MailboxSession session, Mailbox mailbox, MailboxMessage message) throws MailboxException {
        try {
            LOGGER.info("Indexing mailbox {}-{} of user {} on message {}",
                    mailbox.getName(),
                    mailbox.getMailboxId(),
                    session.getUser().getUserName(),
                    message.getUid());
            indexer.indexDocument(indexIdFor(mailbox, message.getUid()), messageToElasticSearchJson.convertToJson(message, ImmutableList.of(session.getUser())));
        } catch (Exception e) {
            try {
                LOGGER.warn(String.format("Indexing mailbox %s-%s of user %s on message %s without attachments ",
                        mailbox.getName(),
                        mailbox.getMailboxId().serialize(),
                        session.getUser().getUserName(),
                        message.getUid().toString()),
                    e);
                indexer.indexDocument(indexIdFor(mailbox, message.getUid()), messageToElasticSearchJson.convertToJsonWithoutAttachment(message, ImmutableList.of(session.getUser())));
            } catch (JsonProcessingException e1) {
                LOGGER.error(String.format("Error when indexing mailbox %s-%s of user %s on message %s without its attachment",
                        mailbox.getName(),
                        mailbox.getMailboxId().serialize(),
                        session.getUser().getUserName(),
                        message.getUid().toString()),
                        e1);
            }
        }
    }
    
    @Override
    public void delete(MailboxSession session, Mailbox mailbox, List<MessageUid> expungedUids) throws MailboxException {
        try {
            indexer.deleteDocuments(expungedUids.stream()
                .map(uid ->  indexIdFor(mailbox, uid))
                .collect(Collectors.toList()));
        } catch (Exception e) {
            LOGGER.error(String.format("Error when deleting messages %s in mailbox %s from index",
                mailbox.getMailboxId().serialize(),
                ImmutableList.copyOf(expungedUids).toString()),
                e);
        }
    }

    @Override
    public void deleteAll(MailboxSession session, Mailbox mailbox) throws MailboxException {
        try {
            indexer.deleteAllMatchingQuery(
                termQuery(
                    JsonMessageConstants.MAILBOX_ID,
                    mailbox.getMailboxId().serialize()));
        } catch (Exception e) {
            LOGGER.error(String.format("Error when deleting all messages in mailbox %s", mailbox.getMailboxId().serialize()), e);
        }
    }

    @Override
    public void update(MailboxSession session, Mailbox mailbox, List<UpdatedFlags> updatedFlagsList) throws MailboxException {
        try {
            indexer.updateDocuments(updatedFlagsList.stream()
                .map(updatedFlags -> createUpdatedDocumentPartFromUpdatedFlags(mailbox, updatedFlags))
                .collect(Collectors.toList()));
        } catch (Exception e) {
            LOGGER.error(String.format("Error when updating index on mailbox %s", mailbox.getMailboxId().serialize()), e);
        }
    }

    public boolean supportsAttachments() {
        return messageToElasticSearchJson.handleIndexAttachment();
    }

    private ElasticSearchIndexer.UpdatedRepresentation createUpdatedDocumentPartFromUpdatedFlags(Mailbox mailbox, UpdatedFlags updatedFlags) {
        try {
            return new ElasticSearchIndexer.UpdatedRepresentation(
                indexIdFor(mailbox, updatedFlags.getUid()),
                    messageToElasticSearchJson.getUpdatedJsonMessagePart(
                        updatedFlags.getNewFlags(),
                        updatedFlags.getModSeq()));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error while creating updatedDocumentParts", e);
        }
    }

    private String indexIdFor(Mailbox mailbox, MessageUid uid) {
        return String.join(ID_SEPARATOR, mailbox.getMailboxId().serialize(), String.valueOf(uid.asLong()));
    }

}
