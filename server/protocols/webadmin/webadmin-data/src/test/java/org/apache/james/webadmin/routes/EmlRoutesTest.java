package org.apache.james.webadmin.routes;

import static io.restassured.RestAssured.with;
import static org.hamcrest.CoreMatchers.is;

import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.exception.MailboxException;
import org.apache.james.mailbox.inmemory.InMemoryId;
import org.apache.james.mailbox.inmemory.manager.InMemoryIntegrationResources;
import org.apache.james.mailbox.model.MailboxId;
import org.apache.james.mailbox.model.MailboxPath;
import org.apache.james.mailbox.store.StoreMailboxManager;
import org.apache.james.webadmin.WebAdminServer;
import org.apache.james.webadmin.WebAdminUtils;
import org.apache.james.webadmin.service.EmlService;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.restassured.RestAssured;
import io.restassured.filter.log.LogDetail;

public class EmlRoutesTest {
    private WebAdminServer webAdminServer;

    private MailboxSession session;
    private StoreMailboxManager mailboxManager;
    private MailboxId.Factory mailboxIdFactory;
    private MailboxId dummyInbox;

    @BeforeEach
    public void setUp() throws Exception {
        mailboxIdFactory = new InMemoryId.Factory();
        mailboxManager = InMemoryIntegrationResources.defaultResources().getMailboxManager();

        webAdminServer = WebAdminUtils
            .createWebAdminServer(new EmlRoutes(new EmlService(mailboxManager, mailboxIdFactory)))
            .start();

        RestAssured.requestSpecification = WebAdminUtils.buildRequestSpecification(webAdminServer)
            .setBasePath(EmlRoutes.BASE_PATH)
            .log(LogDetail.METHOD)
            .build();
    }

    @AfterEach
    public void stop() {
        webAdminServer.destroy();
    }

    @Test
    public void importEmlFileToMailboxShouldReturnNoContentWhenSuccess() throws MailboxException {

        session = mailboxManager.createSystemSession("james@linagora.com");
        dummyInbox = mailboxManager
            .createMailbox(MailboxPath.forUser("james@linagora.com", "inbox"), session)
            .get();

        with()
            .body(ClassLoader.getSystemResource("eml/mail.eml"))
            .post("/" + dummyInbox.serialize() + "/messages")
        .then()
            .statusCode(HttpStatus.NO_CONTENT_204);
    }

    @Test
    public void importEmlToMailBoxShouldAllowOtherFileExtensions() throws MailboxException {

        session = mailboxManager.createSystemSession("james@linagora.com");
        dummyInbox = mailboxManager
            .createMailbox(MailboxPath.forUser("james@linagora.com", "inbox"), session)
            .get();

        with()
            .body(ClassLoader.getSystemResource("eml/mail.json"))
            .post("/" + dummyInbox.serialize() + "/messages")
        .then()
            .statusCode(HttpStatus.NO_CONTENT_204);
    }

    @Test
    public void importEmlToMailboxShouldAllowEmptyEmlPath() throws MailboxException {

        session = mailboxManager.createSystemSession("james@linagora.com");
        dummyInbox = mailboxManager
            .createMailbox(MailboxPath.forUser("james@linagora.com", "inbox"), session)
            .get();

        with()
            .body(ClassLoader.getSystemResource(""))
            .post("/" + dummyInbox.serialize() + "/messages")
        .then()
            .statusCode(HttpStatus.NO_CONTENT_204);
    }

    @Test
    public void importEmlToMailboxShouldReturnBadRequestWhenUsernameDoesNotMatch() throws MailboxException {

        session = mailboxManager.createSystemSession("EML");
        dummyInbox = mailboxManager
            .createMailbox(MailboxPath.forUser("EML", "inbox"), session)
            .get();

        with()
            .body(ClassLoader.getSystemResource("eml/mail.eml"))
            .post("/" + dummyInbox.serialize() + "/messages")
        .then()
            .statusCode(HttpStatus.BAD_REQUEST_400)
            .body("message", is("Unable to create mailbox."));
    }

    @Test
    public void importEmlToMailboxShouldReturnNotFoundWhenPathIsNotCorrect() throws MailboxException {

        session = mailboxManager.createSystemSession("james@linagora.com");
        dummyInbox = mailboxManager
            .createMailbox(MailboxPath.forUser("james@linagora.com", "inbox"), session)
            .get();

        with()
            .body(ClassLoader.getSystemResource("eml/mail.eml"))
            .post("/" + dummyInbox.serialize() + "/message")
        .then()
            .statusCode(HttpStatus.NOT_FOUND_404)
            .body("message", is("POST /mailboxes/1/message can not be found"));
    }
}