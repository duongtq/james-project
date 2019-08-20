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

import static org.apache.james.webadmin.routes.EmlRoutes.MAILBOX_ID;

import java.io.ByteArrayInputStream;

import javax.inject.Inject;

import org.apache.james.mailbox.MailboxManager;
import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.MessageManager;
import org.apache.james.mailbox.exception.BadCredentialsException;
import org.apache.james.mailbox.exception.MailboxException;
import org.apache.james.mailbox.model.MailboxId;
import org.apache.james.webadmin.utils.ErrorResponder;
import org.eclipse.jetty.http.HttpStatus;

import com.google.common.annotations.VisibleForTesting;

import spark.Request;

public class EmlService {

    private final MailboxId.Factory mailboxIdFactory;
    private final MailboxManager mailboxManager;

    @Inject
    @VisibleForTesting
    public EmlService(MailboxManager mailboxManager, MailboxId.Factory mailboxIdFactory) {
        this.mailboxManager = mailboxManager;
        this.mailboxIdFactory = mailboxIdFactory;
    }

    public void importEmlFileToMailbox(Request request) throws MailboxException {

        MailboxSession session = getMailboxSession();
        MessageManager mailbox = getMailboxFromMailboxIdAndSession(request);
        mailbox.appendMessage(MessageManager.AppendCommand.builder()
            .recent()
            .build(new ByteArrayInputStream(request.bodyAsBytes())), session);

    }

    public MailboxSession getMailboxSession() throws MailboxException {
        MailboxSession session;
        try {
            session = mailboxManager.createSystemSession("james@linagora.com");
        } catch (BadCredentialsException e) {
            throw ErrorResponder.builder()
                .statusCode(HttpStatus.BAD_REQUEST_400)
                .type(ErrorResponder.ErrorType.INVALID_ARGUMENT)
                .message("Bad credential.")
                .haltError();
        }
        return session;
    }

    private MessageManager getMailboxFromMailboxIdAndSession(Request request) throws MailboxException {
        String mailboxIdString = request.params(MAILBOX_ID);
        MailboxId mailboxId = mailboxIdFactory.fromString(mailboxIdString);

        MailboxSession session = getMailboxSession();

        MessageManager mailbox;

        try {
            mailbox = mailboxManager.getMailbox(mailboxId, session);
        } catch (MailboxException e) {
            throw ErrorResponder.builder()
                .statusCode(HttpStatus.BAD_REQUEST_400)
                .type(ErrorResponder.ErrorType.INVALID_ARGUMENT)
                .message("Unable to create mailbox.")
                .haltError();
        }
        return mailbox;
    }
}