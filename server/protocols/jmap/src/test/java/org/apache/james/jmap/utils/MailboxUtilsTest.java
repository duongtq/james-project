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

package org.apache.james.jmap.utils;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.james.core.User;
import org.apache.james.mailbox.MailboxManager;
import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.inmemory.manager.InMemoryIntegrationResources;
import org.apache.james.mailbox.model.MailboxId;
import org.apache.james.mailbox.model.MailboxPath;
import org.junit.Before;
import org.junit.Test;

public class MailboxUtilsTest {
    public static final User USER = User.fromUsername("user@domain.org");

    private MailboxManager mailboxManager;
    private MailboxSession mailboxSession;
    private MailboxUtils sut;

    @Before
    public void setup() throws Exception {
        InMemoryIntegrationResources inMemoryIntegrationResources = new InMemoryIntegrationResources();
        mailboxManager = inMemoryIntegrationResources.createMailboxManager(inMemoryIntegrationResources.createGroupMembershipResolver());
        mailboxSession = mailboxManager.login(USER, "pass");
        sut = new MailboxUtils(mailboxManager);
    }
    
    @Test
    public void hasChildrenShouldReturnFalseWhenNoChild() throws Exception {
        MailboxPath mailboxPath = MailboxPath.forUser(USER.asString(), "myBox");
        mailboxManager.createMailbox(mailboxPath, mailboxSession);
        MailboxId mailboxId = mailboxManager.getMailbox(mailboxPath, mailboxSession).getId();

        assertThat(sut.hasChildren(mailboxId, mailboxSession)).isFalse();
    }

    @Test
    public void hasChildrenShouldReturnTrueWhenHasAChild() throws Exception {
        MailboxPath parentMailboxPath = MailboxPath.forUser(USER.asString(), "inbox");
        mailboxManager.createMailbox(parentMailboxPath, mailboxSession);
        MailboxId parentId = mailboxManager.getMailbox(parentMailboxPath, mailboxSession).getId();

        MailboxPath mailboxPath = MailboxPath.forUser(USER.asString(), "inbox.myBox");
        mailboxManager.createMailbox(mailboxPath, mailboxSession);

        assertThat(sut.hasChildren(parentId, mailboxSession)).isTrue();
    }
}
