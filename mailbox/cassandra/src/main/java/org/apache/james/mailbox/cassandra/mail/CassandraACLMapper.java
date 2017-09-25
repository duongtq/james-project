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

package org.apache.james.mailbox.cassandra.mail;

import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;
import static com.datastax.driver.core.querybuilder.QueryBuilder.insertInto;
import static com.datastax.driver.core.querybuilder.QueryBuilder.select;
import static com.datastax.driver.core.querybuilder.QueryBuilder.set;
import static com.datastax.driver.core.querybuilder.QueryBuilder.update;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.apache.james.backends.cassandra.init.CassandraConfiguration;
import org.apache.james.backends.cassandra.utils.CassandraAsyncExecutor;
import org.apache.james.backends.cassandra.utils.CassandraConstants;
import org.apache.james.backends.cassandra.utils.FunctionRunnerWithRetry;
import org.apache.james.backends.cassandra.utils.LightweightTransactionException;
import org.apache.james.mailbox.cassandra.ids.CassandraId;
import org.apache.james.mailbox.cassandra.table.CassandraACLTable;
import org.apache.james.mailbox.cassandra.table.CassandraMailboxTable;
import org.apache.james.mailbox.exception.MailboxException;
import org.apache.james.mailbox.exception.UnsupportedRightException;
import org.apache.james.mailbox.model.MailboxACL;
import org.apache.james.mailbox.model.SimpleMailboxACL;
import org.apache.james.mailbox.store.json.SimpleMailboxACLJsonConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.base.Throwables;

public class CassandraACLMapper {
    private static final Logger LOG = LoggerFactory.getLogger(CassandraACLMapper.class);
    public static final int INITIAL_VALUE = 0;

    @FunctionalInterface
    public interface CodeInjector {
        void inject();
    }

    private final CassandraAsyncExecutor executor;
    private final Session session;
    private final int maxRetry;
    private final CodeInjector codeInjector;

    public CassandraACLMapper(Session session, CassandraConfiguration cassandraConfiguration) {
        this(session, cassandraConfiguration, () -> {});
    }

    public CassandraACLMapper(Session session, CassandraConfiguration cassandraConfiguration, CodeInjector codeInjector) {
        this.session = session;
        this.executor = new CassandraAsyncExecutor(session);
        this.maxRetry = cassandraConfiguration.getAclMaxRetry();
        this.codeInjector = codeInjector;
    }

    public CompletableFuture<MailboxACL> getACL(CassandraId cassandraId) {
        return  getStoredACLRow(cassandraId).thenApply(resultSet -> {
            if (resultSet.isExhausted()) {
                return SimpleMailboxACL.EMPTY;
            }
            String serializedACL = resultSet.one().getString(CassandraACLTable.ACL);
            return deserializeACL(cassandraId, serializedACL);
        });
    }

    public void updateACL(CassandraId cassandraId, MailboxACL.MailboxACLCommand command) throws MailboxException {
        try {
            new FunctionRunnerWithRetry(maxRetry).execute(
                () -> {
                    codeInjector.inject();
                    ResultSet resultSet = getAclWithVersion(cassandraId)
                        .map(aclWithVersion -> aclWithVersion.apply(command))
                        .map(aclWithVersion -> updateStoredACL(cassandraId, aclWithVersion))
                        .orElseGet(() -> insertACL(cassandraId, applyCommandOnEmptyACL(command)));
                    return resultSet.one().getBool(CassandraConstants.LIGHTWEIGHT_TRANSACTION_APPLIED);
                }
            );
        } catch (LightweightTransactionException e) {
            throw new MailboxException("Exception during lightweight transaction", e);
        }
    }

    public void resetACL(CassandraId cassandraId, MailboxACL mailboxACL) {
        try {
            session.execute(
                insertInto(CassandraACLTable.TABLE_NAME)
                    .value(CassandraACLTable.ID, cassandraId.asUuid())
                    .value(CassandraACLTable.ACL, SimpleMailboxACLJsonConverter.toJson(mailboxACL))
                    .value(CassandraACLTable.VERSION, INITIAL_VALUE));
        } catch (JsonProcessingException e) {
            throw Throwables.propagate(e);
        }
    }

    private MailboxACL applyCommandOnEmptyACL(MailboxACL.MailboxACLCommand command) {
        try {
            return SimpleMailboxACL.EMPTY.apply(command);
        } catch (UnsupportedRightException exception) {
            throw Throwables.propagate(exception);
        }
    }

    private CompletableFuture<ResultSet> getStoredACLRow(CassandraId cassandraId) {
        return executor.execute(select(CassandraACLTable.ACL, CassandraACLTable.VERSION)
            .from(CassandraACLTable.TABLE_NAME)
            .where(eq(CassandraMailboxTable.ID, cassandraId.asUuid())));
    }

    private ResultSet updateStoredACL(CassandraId cassandraId, ACLWithVersion aclWithVersion) {
        try {
            return session.execute(
                update(CassandraACLTable.TABLE_NAME)
                    .with(set(CassandraACLTable.ACL, SimpleMailboxACLJsonConverter.toJson(aclWithVersion.mailboxACL)))
                    .and(set(CassandraACLTable.VERSION, aclWithVersion.version + 1))
                    .where(eq(CassandraACLTable.ID, cassandraId.asUuid()))
                    .onlyIf(eq(CassandraACLTable.VERSION, aclWithVersion.version))
            );
        } catch (JsonProcessingException exception) {
            throw Throwables.propagate(exception);
        }
    }

    private ResultSet insertACL(CassandraId cassandraId, MailboxACL acl) {
        try {
            return session.execute(
                insertInto(CassandraACLTable.TABLE_NAME)
                    .value(CassandraACLTable.ID, cassandraId.asUuid())
                    .value(CassandraACLTable.ACL, SimpleMailboxACLJsonConverter.toJson(acl))
                    .value(CassandraACLTable.VERSION, 0)
                    .ifNotExists()
            );
        } catch (JsonProcessingException exception) {
            throw Throwables.propagate(exception);
        }
    }

    private Optional<ACLWithVersion> getAclWithVersion(CassandraId cassandraId) {
        ResultSet resultSet = getStoredACLRow(cassandraId).join();
        if (resultSet.isExhausted()) {
            return Optional.empty();
        }
        Row row = resultSet.one();
        return Optional.of(new ACLWithVersion(row.getLong(CassandraACLTable.VERSION), deserializeACL(cassandraId, row.getString(CassandraACLTable.ACL))));
    }

    private MailboxACL deserializeACL(CassandraId cassandraId, String serializedACL) {
        try {
            return SimpleMailboxACLJsonConverter.toACL(serializedACL);
        } catch(IOException exception) {
            LOG.error("Unable to read stored ACL. " +
                "We will use empty ACL instead." +
                "Mailbox is {} ." +
                "ACL is {}", cassandraId, serializedACL, exception);
            return SimpleMailboxACL.EMPTY;
        }
    }

    private class ACLWithVersion {
        private final long version;
        private final MailboxACL mailboxACL;

        public ACLWithVersion(long version, MailboxACL mailboxACL) {
            this.version = version;
            this.mailboxACL = mailboxACL;
        }

        public ACLWithVersion apply(MailboxACL.MailboxACLCommand command) {
            try {
                return new ACLWithVersion(version, mailboxACL.apply(command));
            } catch(UnsupportedRightException exception) {
                throw Throwables.propagate(exception);
            }
        }
    }
}
