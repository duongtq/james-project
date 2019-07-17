package org.apache.james.webadmin.routes;

import static spark.Spark.halt;

import javax.ws.rs.POST;
import javax.ws.rs.Path;

import org.apache.james.rrt.api.RecipientRewriteTable;
import org.apache.james.rrt.api.RecipientRewriteTableException;
import org.apache.james.rrt.lib.MappingSource;
import org.apache.james.webadmin.Routes;
import org.apache.james.webadmin.dto.RegexMappingDTO;
import org.apache.james.webadmin.utils.ErrorResponder;
import org.apache.james.webadmin.utils.JsonExtractException;
import org.apache.james.webadmin.utils.JsonExtractor;
import org.eclipse.jetty.http.HttpStatus;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import spark.HaltException;
import spark.Request;
import spark.Response;
import spark.Service;

public class RegexMappingRoutes implements Routes {

    static final String BASE_PATH = "/mappings/regex";

    private final RecipientRewriteTable recipientRewriteTable;
    private final JsonExtractor<RegexMappingDTO> jsonExtractor;

    RegexMappingRoutes(RecipientRewriteTable recipientRewriteTable) {
        this.recipientRewriteTable = recipientRewriteTable;
        this.jsonExtractor = new JsonExtractor<>(RegexMappingDTO.class);
    }

    @Override
    public String getBasePath() {
        return BASE_PATH;
    }

    @Override
    public void define(Service service) {
        service.post(getBasePath(), this::addRegexMapping);
    }

    @POST
    @Path(BASE_PATH)
    @ApiOperation(value = "adding address-regex mappings to RecipientRewriteTable")
    @ApiResponses(value = {
        @ApiResponse(code = HttpStatus.NO_CONTENT_204, message = "No body created"),
        @ApiResponse(code = HttpStatus.BAD_REQUEST_400, message = "Invalid body")
    })
    public HaltException addRegexMapping(Request request, Response response) throws JsonExtractException {
        RegexMappingDTO regexMappingDTO = jsonExtractor.parse(request.body());
        MappingSource mappingSource = extractMappingSource(regexMappingDTO);

        try {
            recipientRewriteTable.addRegexMapping(mappingSource, regexMappingDTO.getRegex());
        } catch (RecipientRewriteTableException e) {
            throw ErrorResponder.builder()
                .statusCode(HttpStatus.INTERNAL_SERVER_ERROR_500)
                .type(ErrorResponder.ErrorType.SERVER_ERROR)
                .message(e.getMessage())
                .haltError();
        }
        return halt(HttpStatus.NO_CONTENT_204);
    }

    // Catch IllegalArgumentException here to improve the response message
    private MappingSource extractMappingSource(RegexMappingDTO regexMappingDTO) {
        try {
            return MappingSource.parse(regexMappingDTO.getSource());
        } catch (IllegalArgumentException e) {
            throw ErrorResponder.builder()
                .statusCode(HttpStatus.BAD_REQUEST_400)
                .type(ErrorResponder.ErrorType.INVALID_ARGUMENT)
                .message("Invalid `source` field.")
                .haltError();
        }
    }
}