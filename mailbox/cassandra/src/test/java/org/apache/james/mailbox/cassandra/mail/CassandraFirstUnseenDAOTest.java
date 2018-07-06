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

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.james.mailbox.MessageUid;
import org.apache.james.mailbox.cassandra.ids.CassandraId;
import org.junit.jupiter.api.Test;

public interface CassandraFirstUnseenDAOTest {
    
    CassandraId MAILBOX_ID = CassandraId.timeBased();
    MessageUid UID_1 = MessageUid.of(1);
    MessageUid UID_2 = MessageUid.of(2);

    CassandraFirstUnseenDAO testee();

    @Test
    default void retrieveFirstUnreadShouldReturnEmptyByDefault() {
        assertThat(testee().retrieveFirstUnread(MAILBOX_ID).join().isPresent())
            .isFalse();
    }

    @Test
    default void addUnreadShouldThenBeReportedAsFirstUnseen() {
        testee().addUnread(MAILBOX_ID, UID_1).join();

        assertThat(testee().retrieveFirstUnread(MAILBOX_ID).join())
            .contains(UID_1);
    }

    @Test
    default void retrieveFirstUnreadShouldReturnLowestUnreadUid() {
        testee().addUnread(MAILBOX_ID, UID_1).join();

        testee().addUnread(MAILBOX_ID, UID_2).join();

        assertThat(testee().retrieveFirstUnread(MAILBOX_ID).join())
            .contains(UID_1);
    }

    @Test
    default void retrieveFirstUnreadShouldBeOrderIndependent() {
        testee().addUnread(MAILBOX_ID, UID_2).join();

        testee().addUnread(MAILBOX_ID, UID_1).join();

        assertThat(testee().retrieveFirstUnread(MAILBOX_ID).join())
            .contains(UID_1);
    }

    @Test
    default void addUnreadShouldBeIdempotent() {
        testee().addUnread(MAILBOX_ID, UID_1).join();

        testee().addUnread(MAILBOX_ID, UID_1).join();

        assertThat(testee().retrieveFirstUnread(MAILBOX_ID).join())
            .contains(UID_1);
    }


    @Test
    default void removeUnreadShouldReturnWhenNoData() {
        testee().removeUnread(MAILBOX_ID, UID_1).join();

        assertThat(testee().retrieveFirstUnread(MAILBOX_ID).join())
            .isEmpty();
    }

    @Test
    default void removeUnreadShouldRemoveOnlyUnread() {
        testee().addUnread(MAILBOX_ID, UID_1).join();

        testee().removeUnread(MAILBOX_ID, UID_1).join();

        assertThat(testee().retrieveFirstUnread(MAILBOX_ID).join())
            .isEmpty();
    }

    @Test
    default void removeUnreadShouldRemoveLastUnread() {
        testee().addUnread(MAILBOX_ID, UID_1).join();
        testee().addUnread(MAILBOX_ID, UID_2).join();

        testee().removeUnread(MAILBOX_ID, UID_2).join();

        assertThat(testee().retrieveFirstUnread(MAILBOX_ID).join())
            .contains(UID_1);
    }

    @Test
    default void removeUnreadShouldHaveNoEffectWhenNotLast() {
        testee().addUnread(MAILBOX_ID, UID_1).join();
        testee().addUnread(MAILBOX_ID, UID_2).join();

        testee().removeUnread(MAILBOX_ID, UID_1).join();

        assertThat(testee().retrieveFirstUnread(MAILBOX_ID).join())
            .contains(UID_2);
    }
}
