package org.apache.james.webadmin.routes;

import static io.restassured.RestAssured.with;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.is;
import static org.mockito.Mockito.mock;

import org.apache.james.core.Domain;
import org.apache.james.core.User;
import org.apache.james.dnsservice.api.DNSService;
import org.apache.james.domainlist.api.DomainList;
import org.apache.james.domainlist.memory.MemoryDomainList;
import org.apache.james.rrt.lib.Mapping;
import org.apache.james.rrt.lib.MappingSource;
import org.apache.james.rrt.memory.MemoryRecipientRewriteTable;
import org.apache.james.webadmin.WebAdminServer;
import org.apache.james.webadmin.WebAdminUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.restassured.RestAssured;
import io.restassured.filter.log.LogDetail;
import io.restassured.http.ContentType;

class RegexMappingRoutesTest {

    private WebAdminServer webAdminServer;
    private MemoryRecipientRewriteTable memoryRecipientRewriteTable;

    @BeforeEach
    void beforeEach() throws Exception {
        DNSService dnsService = mock(DNSService.class);
        DomainList domainList = new MemoryDomainList(dnsService);
        memoryRecipientRewriteTable = new MemoryRecipientRewriteTable();
        memoryRecipientRewriteTable.setDomainList(domainList);
        domainList.addDomain(Domain.of("domain.tld"));

        webAdminServer = WebAdminUtils
            .createWebAdminServer(new RegexMappingRoutes(memoryRecipientRewriteTable))
            .start();

        RestAssured.requestSpecification = WebAdminUtils.buildRequestSpecification(webAdminServer)
            .setBasePath(RegexMappingRoutes.BASE_PATH)
            .log(LogDetail.METHOD)
            .build();
    }

    @AfterEach
    void stop() {
        webAdminServer.destroy();
    }

    @Test
    void addRegexMappingShouldReturnNoContentWhenSuccess() {
        with()
            .body(
                "{" +
                "  \"source\": \"james@domain.tld\"," +
                "  \"regex\": \"^[aeiou]\"" +
                "}")
            .post()
        .then()
            .statusCode(204)
            .contentType(ContentType.JSON);

        assertThat(memoryRecipientRewriteTable
            .getStoredMappings(MappingSource.fromUser(User.fromUsername("james@domain.tld"))))
            .containsOnly(Mapping.regex("^[aeiou]"));
    }

    @Test
    void addRegexMappingShouldReturnBadRequestWhenBodyIsInvalid() {
        with()
            .body("Invalid body")
            .post()
        .then()
            .statusCode(400)
            .contentType(ContentType.JSON);
    }

    @Test
    void addRegexMappingShouldReturnBadRequestWhenSourceIsEmpty() {
        with()
            .body(
                "{" +
                "  \"source\": \"\"," +
                "  \"regex\": \"^[aeiou]\"" +
                "}")
            .post()
        .then()
            .statusCode(400)
            .contentType(ContentType.JSON)
            .body("message", is("Invalid `source` field."));
    }

    @Test
    void addRegexMappingShouldAllowEmptyRegex() {
        with()
            .body(
                "{" +
                "  \"source\":\"james@domain.tld\"," +
                "  \"regex\": \"\"" +
                "}")
            .post()
        .then()
            .statusCode(204)
            .contentType(ContentType.JSON);
    }

    @Test
    void addRegexMappingShouldReturnBadRequestWhenSourceAndRegexEmpty() {
        with()
            .body(
                "{" +
                "  \"source\": \"\"," +
                "  \"regex\": \"\"" +
                "}")
            .post()
        .then() // Compare response info
            .statusCode(400)
            .contentType(ContentType.JSON).body("message", is("Invalid `source` field."));
    }

    @Test
    void addRegexMappingShouldReturnBadRequestWhenSourceIsNull() {
        with()
            .body(
                "{" +
                "  \"source\": null," +
                "  \"regex\": \"yourRegexExpression\"" +
                "}")
            .post()
        .then()
            .statusCode(400)
            .contentType(ContentType.JSON);
    }

    @Test
    void addRegexMappingShouldReturnBadRequestWhenRegexIsNull() {
        with()
            .body(
                "{" +
                "  \"source\":\"james@domain.tld\"," +
                "  \"regex\": null" +
                "}")
            .post()
        .then()
            .statusCode(400)
            .contentType(ContentType.JSON);
    }

    @Test
    void addRegexMappingShouldReturnBadRequestWhenSourceAndRegexIsNull() {
        with()
            .body(
                "{" +
                "  \"source\":null," +
                "  \"regex\": null" +
                "}")
            .post()
        .then()
            .statusCode(400)
            .contentType(ContentType.JSON);
    }

    @Test
    void addRegexMappingShouldAllowUserWithoutDomain() {
        with()
            .body(
                "{" +
                "  \"source\":\"jamesdomaintld\"," +
                "  \"regex\": \"^[aeiou]\"" +
                "}")
            .post()
        .then()
            .statusCode(204)
            .contentType(ContentType.JSON);

        assertThat(memoryRecipientRewriteTable
            .getStoredMappings(MappingSource.fromUser(User.fromUsername("jamesdomaintld"))))
            .containsOnly(Mapping.regex("^[aeiou]"));
    }
}