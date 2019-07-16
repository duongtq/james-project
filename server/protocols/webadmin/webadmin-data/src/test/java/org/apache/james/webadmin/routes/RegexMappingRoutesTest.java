package org.apache.james.webadmin.routes;

import io.restassured.RestAssured;
import io.restassured.filter.log.LogDetail;
import io.restassured.http.ContentType;
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

import static io.restassured.RestAssured.with;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

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
    void addRegexMappingRoutesShouldReturnNoContentWhenNoBodyOnCreated() {
        with()
            .body(
                "{" +
                "  \"source\": \"abc@domain.tld\"," +
                "  \"regex\": \"^[aeiou]\"" +
                "}")
            .post()
        .then()
            .statusCode(204)
            .contentType(ContentType.JSON);

        assertThat(memoryRecipientRewriteTable
            .getStoredMappings(MappingSource.fromUser(User.fromUsername("abc@domain.tld"))))
            .containsOnly(Mapping.regex("^[aeiou]"));
    }

    @Test
    void addRegexMappingRoutesShouldReturnBadRequestWhenBodyIsInvalid() {
        with()
            .body("Invalid body")
            .post()
        .then()
            .statusCode(400)
            .contentType(ContentType.JSON);
    }

    @Test
    void addRegexMappingRoutesShouldReturnBadRequestWhenSourceIsEmpty() {
        with()
            .body(
                "{" +
                "  \"source\": \"\"," +
                "  \"regex\": \"^[aeiou]\"" +
                "}")
            .post()
        .then()
            .statusCode(400)
            .contentType(ContentType.JSON);
    }

    @Test
    void addRegexMappingShouldAllowEmptyRegex() {
        with()
            .body(
                "{" +
                "  \"source\":\"abc@domain.tld\"," +
                "  \"regex\": \"\"" +
                "}")
                .post()
        .then()
            .statusCode(204)
            .contentType(ContentType.JSON);
    }

    @Test
    void addRegexMappingRoutesShouldReturnBadRequestWhenSourceAndRegexEmpty() {
        with()
            .body(
                "{" +
                "  \"source\": \"\"," +
                "  \"regex\": \"\"" +
                "}")
                .post()
        .then()
            .statusCode(400)
            .contentType(ContentType.JSON);
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
                "  \"source\":\"abc@domain.tld\"," +
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
                "  \"source\":\"abcdomaintld\"," +
                "  \"regex\": \"^[aeiou]\"" +
                "}")
            .post()
        .then()
            .statusCode(204)
            .contentType(ContentType.JSON);

        assertThat(memoryRecipientRewriteTable
            .getStoredMappings(MappingSource.fromUser(User.fromUsername("abcdomaintld"))))
            .containsOnly(Mapping.regex("^[aeiou]"));
    }
}