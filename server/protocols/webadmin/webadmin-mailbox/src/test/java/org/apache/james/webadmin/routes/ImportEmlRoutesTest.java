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

package org.apache.james.webadmin.routes;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.is;

import java.nio.charset.Charset;

import org.apache.commons.io.IOUtils;
import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.inmemory.InMemoryId;
import org.apache.james.mailbox.inmemory.manager.InMemoryIntegrationResources;
import org.apache.james.mailbox.model.FetchGroupImpl;
import org.apache.james.mailbox.model.MailboxId;
import org.apache.james.mailbox.model.MailboxPath;
import org.apache.james.mailbox.model.MessageRange;
import org.apache.james.mailbox.store.StoreMailboxManager;
import org.apache.james.webadmin.WebAdminServer;
import org.apache.james.webadmin.WebAdminUtils;
import org.apache.james.webadmin.service.ImportEmlService;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.github.fge.lambdas.Throwing;

import io.restassured.RestAssured;
import io.restassured.filter.log.LogDetail;
import io.restassured.http.ContentType;

public class ImportEmlRoutesTest {
    private WebAdminServer webAdminServer;

    private MailboxSession session;
    private StoreMailboxManager mailboxManager;
    private MailboxId.Factory mailboxIdFactory;
    private MailboxId dummyInbox;

    @BeforeEach
    public void setUp() {
        mailboxIdFactory = new InMemoryId.Factory();
        mailboxManager = InMemoryIntegrationResources.defaultResources().getMailboxManager();

        webAdminServer = WebAdminUtils
            .createWebAdminServer(new ImportEmlRoutes(new ImportEmlService(mailboxManager, mailboxIdFactory)))
            .start();

        RestAssured.requestSpecification = WebAdminUtils.buildRequestSpecification(webAdminServer)
            .setBasePath(ImportEmlRoutes.BASE_PATH)
            .log(LogDetail.ALL)
            .build();
    }

    @AfterEach
    public void stop() {
        webAdminServer.destroy();
    }

    @Test
    public void importEmlFileToMailboxShouldReturnNoContentWhenSuccess() throws Exception {
        session = mailboxManager.createSystemSession("EmlService");
        dummyInbox = mailboxManager
            .createMailbox(MailboxPath.forUser("EmlService", "inbox"), session)
            .get();

        given()
            .body(IOUtils.toString(ClassLoader.getSystemResourceAsStream("importEmlTest.eml"), Charset.forName("UTF-8")))
        .when()
            .post("/" + dummyInbox.serialize() + "/messages")
        .then()
            .statusCode(HttpStatus.NO_CONTENT_204);

        assertThat(mailboxManager.getMailbox(MailboxPath.forUser("EmlService", "inbox"), session)
            .getMessages(MessageRange.all(), FetchGroupImpl.FULL_CONTENT, session))
            .toIterable()
            .hasSize(1)
            .allSatisfy(Throwing
                .consumer(result -> assertThat(result
                    .getBody()
                    .getInputStream())
                    .hasSameContentAs(ClassLoader.getSystemResourceAsStream("importEmlTest.eml"))));
    }

    @Test
    public void importEmlToMailboxShouldReturnNotFoundWhenUsernameDoesNotMatch() throws Exception {
        session = mailboxManager.createSystemSession("EML");
        dummyInbox = mailboxManager
            .createMailbox(MailboxPath.forUser("EML", "inbox"), session)
            .get();

        given()
            .body(IOUtils.toString(ClassLoader.getSystemResourceAsStream("importEmlTest.eml"), Charset.forName("UTF-8")))
        .with()
            .post("/" + dummyInbox.serialize() + "/messages")
        .then()
            .statusCode(HttpStatus.NOT_FOUND_404)
            .body("message", is("Unable to retrieve mailbox."));
    }

    @Test
    public void importEmlToMailboxShouldReturnNotFoundWhenRequestURLIsNotCorrect() throws Exception {
        session = mailboxManager.createSystemSession("EmlService");
        dummyInbox = mailboxManager
            .createMailbox(MailboxPath.forUser("EmlService", "inbox"), session)
            .get();

        given()
            .body(IOUtils.toString(ClassLoader.getSystemResourceAsStream("importEmlTest.eml"), Charset.forName("UTF-8")))
        .with()
            .post("/" + dummyInbox.serialize() + "/message")
        .then()
            .statusCode(HttpStatus.NOT_FOUND_404)
            .body("message", is("POST /mailboxes/1/message can not be found"));
    }

    @Test
    public void importEmlToMailboxShouldReturnNotFoundWhenSessionError() throws Exception {
        session = mailboxManager.createSystemSession("ErrorSession");
        dummyInbox = mailboxManager
            .createMailbox(MailboxPath.forUser("ErrorSession", "inbox"), session)
            .get();

        given()
            .body(IOUtils.toString(ClassLoader.getSystemResourceAsStream("importEmlTest.eml"), Charset.forName("UTF-8")))
        .with()
            .post("/" + dummyInbox.serialize() + "/messages")
        .then()
            .statusCode(HttpStatus.NOT_FOUND_404)
            .body("message", is("Unable to retrieve mailbox."));
    }

    @Test
    public void importEmlToMailboxShouldReturnServerErrorWhenFileSizeExceedsTheLimit() throws Exception {
        session = mailboxManager.createSystemSession("EmlService");
        dummyInbox = mailboxManager
            .createMailbox(MailboxPath.forUser("EmlService", "inbox"), session)
            .get();

        given()
            .body(IOUtils.toString(ClassLoader.getSystemResourceAsStream("hugeFileTest.eml"), Charset.forName("UTF-8")))
        .with()
            .post("/" + dummyInbox.serialize() + "/messages")
        .then()
            .statusCode(HttpStatus.PAYLOAD_TOO_LARGE_413)
            .contentType(ContentType.JSON)
            .body("message", is("File size exceed the limit (2.56MB)"));
    }
}