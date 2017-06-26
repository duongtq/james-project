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

package org.apache.james.mpt.imapmailbox.suite;

import java.util.Locale;

import org.apache.james.mailbox.model.MailboxPath;
import org.apache.james.mailbox.model.SimpleMailboxACL.Rfc4314Rights;
import org.apache.james.mpt.api.ImapHostSystem;
import org.apache.james.mpt.imapmailbox.GrantRightsOnHost;
import org.apache.james.mpt.imapmailbox.ImapTestConstants;
import org.apache.james.mpt.imapmailbox.MailboxMessageAppender;
import org.junit.Before;
import org.junit.Test;

public abstract class ACLIntegration implements ImapTestConstants {
    public static final String OTHER_USER_NAME = "Boby";
    public static final String OTHER_USER_PASSWORD = "password";
    public static final MailboxPath OTHER_USER_MAILBOX = new MailboxPath("#private", OTHER_USER_NAME, "");
    public static final MailboxPath MY_INBOX = new MailboxPath("#private", USER, "");

    protected abstract ImapHostSystem createImapHostSystem();
    protected abstract GrantRightsOnHost createGrantRightsOnHost();
    protected abstract MailboxMessageAppender createMailboxMessageAppender();
    
    private ImapHostSystem system;
    private GrantRightsOnHost grantRightsOnHost;
    private MailboxMessageAppender mailboxMessageAppender;

    private ACLScriptedTestProtocol scriptedTestProtocol;

    @Before
    public void setUp() throws Exception {
        system = createImapHostSystem();
        grantRightsOnHost = createGrantRightsOnHost();
        mailboxMessageAppender = createMailboxMessageAppender();
        scriptedTestProtocol = new ACLScriptedTestProtocol(grantRightsOnHost, mailboxMessageAppender, "/org/apache/james/imap/scripts/", system)
                .withUser(USER, PASSWORD)
                .withUser(OTHER_USER_NAME, OTHER_USER_PASSWORD)
                .withLocale(Locale.US);
    }
    
    @Test
    public void rightRShouldBeSufficientToPerformStatusSelectCloseExamine() throws Exception {
        scriptedTestProtocol
            .withGrantRights(OTHER_USER_MAILBOX, USER, new Rfc4314Rights("r"))
            .run("aclIntegration/ACLIntegrationRightR");
    }

    @Test
    public void rightRShouldBeNeededToPerformStatusSelectCloseExamine() throws Exception {
        scriptedTestProtocol
            .withGrantRights(OTHER_USER_MAILBOX, USER, new Rfc4314Rights("lswipkxtecda"))
            .run("aclIntegration/ACLIntegrationWithoutRightR");
    }

    @Test
    public void rightLShouldBeSufficientToPerformList() throws Exception {
        scriptedTestProtocol
            .withGrantRights(OTHER_USER_MAILBOX, USER, new Rfc4314Rights("l"))
            .run("aclIntegration/ACLIntegrationRightL");
    }

    @Test
    public void rightLShouldBeNeededToPerformListLsubSubscribe() throws Exception {
        scriptedTestProtocol
            .withGrantRights(OTHER_USER_MAILBOX, USER, new Rfc4314Rights("rswipkxtecda"))
            .run("aclIntegration/ACLIntegrationWithoutRightL");
    }

    @Test
    public void rightAShouldBeSufficientToManageACL() throws Exception {
        scriptedTestProtocol
            .withGrantRights(OTHER_USER_MAILBOX, USER, new Rfc4314Rights("a"))
            .run("aclIntegration/ACLIntegrationRightA");
    }

    @Test
    public void rightAShouldBeNeededToManageACL() throws Exception {
        scriptedTestProtocol
            .withGrantRights(OTHER_USER_MAILBOX, USER, new Rfc4314Rights("rswipkxtecdl"))
            .run("aclIntegration/ACLIntegrationWithoutRightA");
    }

    @Test
    public void rightXOnOriginShouldBeSufficientToRenameAMailbox() throws Exception {
        scriptedTestProtocol
            .withMailbox(new MailboxPath("#private","Boby","test"))
            .withGrantRights(new MailboxPath("#private", OTHER_USER_NAME, "test"), USER, new Rfc4314Rights("x"))
            .run("aclIntegration/ACLIntegrationRightX");
    }

    @Test
    public void rightXOnOriginShouldBeNeededToRenameAMailbox() throws Exception {
        scriptedTestProtocol
            .withMailbox(new MailboxPath("#private","Boby","test"))
            .withGrantRights(new MailboxPath("#private", OTHER_USER_NAME, "test"), USER, new Rfc4314Rights("rswipktela"))
            .run("aclIntegration/ACLIntegrationWithoutRightX");
    }

    @Test
    public void rightKOnDestinationShouldBeSufficientToRenameAMailbox() throws Exception {
        MailboxPath newMailbox = new MailboxPath("#private", USER, "test");
        scriptedTestProtocol
            .withMailbox(newMailbox)
            .withGrantRights(newMailbox, USER, new Rfc4314Rights("x"))
            .withGrantRights(OTHER_USER_MAILBOX, USER, new Rfc4314Rights("k"))
            .run("aclIntegration/ACLIntegrationRightK");
    }

    @Test
    public void rightKOnDestinationShouldBeNeededToRenameAMailbox() throws Exception {
        MailboxPath newMailbox = new MailboxPath("#private", USER, "test");
        scriptedTestProtocol
            .withMailbox(newMailbox)
            .withGrantRights(newMailbox, USER, new Rfc4314Rights("x"))
            .withGrantRights(OTHER_USER_MAILBOX, USER, new Rfc4314Rights("rswipxtela"))
            .run("aclIntegration/ACLIntegrationWithoutRightK");
    }

    @Test
    public void rightREShouldBeSufficientToPerformExpunge() throws Exception {
        scriptedTestProtocol
            .withGrantRights(OTHER_USER_MAILBOX, USER, new Rfc4314Rights("re"))
            .run("aclIntegration/ACLIntegrationRightRE");
    }

    @Test
    public void rightEShouldBeNeededToPerformExpunge() throws Exception {
        scriptedTestProtocol
            .withGrantRights(OTHER_USER_MAILBOX, USER, new Rfc4314Rights("rswipxtclak"))
            .run("aclIntegration/ACLIntegrationWithoutRightE");
    }

    @Test
    public void rightIShouldBeSufficientToPerformAppend() throws Exception {
        scriptedTestProtocol
            .withGrantRights(OTHER_USER_MAILBOX, USER, new Rfc4314Rights("ri"))
            .run("aclIntegration/ACLIntegrationRightI");
    }

    @Test
    public void rightIShouldBeNeededToPerformAppend() throws Exception {
        scriptedTestProtocol
            .withGrantRights(OTHER_USER_MAILBOX, USER, new Rfc4314Rights("rswepxtcdlak"))
            .run("aclIntegration/ACLIntegrationWithoutRightI");
    }

    @Test
    public void rightISShouldBeSufficientToPerformAppendOfSeenMessage() throws Exception {
        scriptedTestProtocol
            .withGrantRights(OTHER_USER_MAILBOX, USER, new Rfc4314Rights("ris"))
            .run("aclIntegration/ACLIntegrationRightIS");
    }

    @Test
    public void rightITShouldBeSufficientToPerformAppendOfDeletedMessage() throws Exception {
        scriptedTestProtocol
            .withGrantRights(OTHER_USER_MAILBOX, USER, new Rfc4314Rights("rit"))
            .run("aclIntegration/ACLIntegrationRightIT");
    }

    @Test
    public void rightIWShouldBeSufficientToPerformAppendOfDeletedMessage() throws Exception {
        scriptedTestProtocol
            .withGrantRights(OTHER_USER_MAILBOX, USER, new Rfc4314Rights("riw"))
            .run("aclIntegration/ACLIntegrationRightIW");
    }

    @Test
    public void rightRSShouldBeSufficientToPerformStoreAndFetchOnSeenMessage() throws Exception {
        scriptedTestProtocol
            .withGrantRights(OTHER_USER_MAILBOX, USER, new Rfc4314Rights("rs"))
            .withFilledMailbox(OTHER_USER_MAILBOX)
            .run("aclIntegration/ACLIntegrationRightRS");
    }

    @Test
    public void rightSShouldBeNeededToPerformStoreAndFetchOnSeenMessage() throws Exception {
        scriptedTestProtocol
            .withGrantRights(OTHER_USER_MAILBOX, USER, new Rfc4314Rights("rwipxtcdlake"))
            .withFilledMailbox(OTHER_USER_MAILBOX)
            .run("aclIntegration/ACLIntegrationWithoutRightS");
    }

    @Test
    public void rightRWShouldBeSufficientToPerformStoreOnFlaggedMessage() throws Exception {
        scriptedTestProtocol
            .withGrantRights(OTHER_USER_MAILBOX, USER, new Rfc4314Rights("rw"))
            .withFilledMailbox(OTHER_USER_MAILBOX)
            .run("aclIntegration/ACLIntegrationRightRW");
    }

    @Test
    public void rightWShouldBeNeededToPerformStoreOnFlaggedMessage() throws Exception {
        scriptedTestProtocol
            .withGrantRights(OTHER_USER_MAILBOX, USER, new Rfc4314Rights("rsipxtcdlake"))
            .withFilledMailbox(OTHER_USER_MAILBOX)
            .run("aclIntegration/ACLIntegrationWithoutRightW");
    }

    @Test
    public void rightRTShouldBeSufficientToPerformStoreOnDeletedMessage() throws Exception {
        scriptedTestProtocol
            .withGrantRights(OTHER_USER_MAILBOX, USER, new Rfc4314Rights("rt"))
            .withFilledMailbox(OTHER_USER_MAILBOX)
            .run("aclIntegration/ACLIntegrationRightRT");
    }

    @Test
    public void rightTShouldBeNeededToPerformStoreOnFlaggedMessage() throws Exception {
        scriptedTestProtocol
            .withGrantRights(OTHER_USER_MAILBOX, USER, new Rfc4314Rights("rwipxslake"))
            .withFilledMailbox(OTHER_USER_MAILBOX)
            .run("aclIntegration/ACLIntegrationWithoutRightT");
    }

    @Test
    public void rightIShouldBeSufficientToPerformCopy() throws Exception {
        scriptedTestProtocol
            .withGrantRights(OTHER_USER_MAILBOX, USER, new Rfc4314Rights("i"))
            .withFilledMailbox(MY_INBOX)
            .run("aclIntegration/ACLIntegrationCopyI");
    }

    @Test
    public void rightIShouldBeNeededToPerformCopy() throws Exception {
        scriptedTestProtocol
            .withGrantRights(OTHER_USER_MAILBOX, USER, new Rfc4314Rights("rswpxtcdlake"))
            .withFilledMailbox(MY_INBOX)
            .run("aclIntegration/ACLIntegrationCopyWithoutI");
    }

    @Test
    public void rightIShouldBeSufficientToPerformOfSeenMessagesCopy() throws Exception {
        scriptedTestProtocol
            .withGrantRights(OTHER_USER_MAILBOX, USER, new Rfc4314Rights("ris"))
            .withFilledMailbox(MY_INBOX)
            .run("aclIntegration/ACLIntegrationCopyIS");
    }

    @Test
    public void rightSShouldBeNeededToPerformCopyOfSeenMessage() throws Exception {
        scriptedTestProtocol
            .withGrantRights(OTHER_USER_MAILBOX, USER, new Rfc4314Rights("riwpxtcdlake"))
            .withFilledMailbox(MY_INBOX)
            .run("aclIntegration/ACLIntegrationCopyWithoutS");
    }

    @Test
    public void rightIWShouldBeSufficientToPerformOfFlaggedMessagesCopy() throws Exception {
        scriptedTestProtocol
            .withGrantRights(OTHER_USER_MAILBOX, USER, new Rfc4314Rights("riw"))
            .withFilledMailbox(MY_INBOX)
            .run("aclIntegration/ACLIntegrationCopyIW");
    }

    @Test
    public void rightWShouldBeNeededToPerformCopyOfFlaggedMessage() throws Exception {
        scriptedTestProtocol
            .withGrantRights(OTHER_USER_MAILBOX, USER, new Rfc4314Rights("rispxtcdlake"))
            .withFilledMailbox(MY_INBOX)
            .run("aclIntegration/ACLIntegrationCopyWithoutW");
    }

    @Test
    public void rightITShouldBeSufficientToPerformOfDeletedMessagesCopy() throws Exception {
        scriptedTestProtocol
            .withGrantRights(OTHER_USER_MAILBOX, USER, new Rfc4314Rights("rit"))
            .withFilledMailbox(MY_INBOX)
            .run("aclIntegration/ACLIntegrationCopyIT");
    }

    @Test
    public void rightTShouldBeNeededToPerformCopyOfDeletedMessage() throws Exception {
        scriptedTestProtocol
            .withGrantRights(OTHER_USER_MAILBOX, USER, new Rfc4314Rights("rispxwlake"))
            .withFilledMailbox(MY_INBOX)
            .run("aclIntegration/ACLIntegrationCopyWithoutT");
    }
}
