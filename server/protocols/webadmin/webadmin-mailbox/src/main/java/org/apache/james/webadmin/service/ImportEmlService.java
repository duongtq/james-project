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

package org.apache.james.webadmin.service;

import javax.inject.Inject;

import org.apache.james.mailbox.MailboxManager;
import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.MessageManager;
import org.apache.james.mailbox.exception.MailboxException;
import org.apache.james.mailbox.model.MailboxId;

import com.google.common.annotations.VisibleForTesting;

public class ImportEmlService {

    private final MailboxId.Factory mailboxIdFactory;
    private final MailboxManager mailboxManager;

    @Inject
    @VisibleForTesting
    public ImportEmlService(MailboxManager mailboxManager, MailboxId.Factory mailboxIdFactory) {
        this.mailboxManager = mailboxManager;
        this.mailboxIdFactory = mailboxIdFactory;
    }

    public MailboxSession getMailboxSession() throws MailboxException {
        MailboxSession webAdminImportEMLSession = mailboxManager.createSystemSession("EmlService");
        return webAdminImportEMLSession;
    }

    public MessageManager retrieveMailbox(String mailboxIdParameter) throws MailboxException {
        MailboxId mailboxId = mailboxIdFactory.fromString(mailboxIdParameter);
        MailboxSession session = getMailboxSession();

        MessageManager mailbox = mailboxManager.getMailbox(mailboxId, session);
        return mailbox;
    }
}