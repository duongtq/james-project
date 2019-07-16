package org.apache.james.webadmin.routes;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.apache.james.rrt.api.RecipientRewriteTable;
import org.apache.james.rrt.api.RecipientRewriteTableException;
import org.apache.james.rrt.lib.MappingSource;
import org.apache.james.webadmin.Routes;
import org.apache.james.webadmin.dto.AddRegexMappingRequestBodyDTO;
import org.apache.james.webadmin.utils.ErrorResponder;
import org.apache.james.webadmin.utils.JsonExtractor;
import org.eclipse.jetty.http.HttpStatus;
import spark.HaltException;
import spark.Request;
import spark.Response;
import spark.Service;
import javax.ws.rs.POST;
import javax.ws.rs.Path;

import static spark.Spark.halt;

public class RegexMappingRoutes implements Routes {
    public static final String BASE_PATH = "/mappings/regex";
    private final RecipientRewriteTable recipientRewriteTable;
    private final JsonExtractor<AddRegexMappingRequestBodyDTO> jsonExtractor;

    // Constructor
    public RegexMappingRoutes(RecipientRewriteTable recipientRewriteTable) {
        this.recipientRewriteTable = recipientRewriteTable;
        this.jsonExtractor = new JsonExtractor<>(AddRegexMappingRequestBodyDTO.class);
    }

    @Override
    public String getBasePath() {
        return BASE_PATH;
    }

    @Override
    public void define(Service service) {
        service.post(getBasePath(), this::addRegexMappingRoutes );
    }

    @POST
    @Path(BASE_PATH)
    @ApiOperation(value = "adding address-regex mappings to RecipientRewriteTable")
    @ApiResponses(value = {
            @ApiResponse(code = HttpStatus.NO_CONTENT_204, message = "No body created"),
            @ApiResponse(code = HttpStatus.BAD_REQUEST_400, message = "Invalid body"),
            @ApiResponse(code = HttpStatus.INTERNAL_SERVER_ERROR_500, message = "Internal server error " +
                    "- Something went bad on the server side.")
    })

    public HaltException addRegexMappingRoutes(Request request, Response response) throws Exception {
        MappingSource mappingSource = toMappingSource(request);
        String regex = toRegex(request);

        // Handle null values
        if ( mappingSource == null || regex == null || regex == "" ) {
            return halt(HttpStatus.BAD_REQUEST_400);
        }

        try {
            recipientRewriteTable.addRegexMapping(mappingSource, regex);
        } catch (RecipientRewriteTableException e) {
            throw ErrorResponder.builder()
                    .statusCode(HttpStatus.INTERNAL_SERVER_ERROR_500)
                    .type(ErrorResponder.ErrorType.SERVER_ERROR)
                    .message(e.getMessage())
                    .haltError();
        }
        return halt(HttpStatus.NO_CONTENT_204);
    }

    public MappingSource toMappingSource(Request request) throws Exception {
        AddRegexMappingRequestBodyDTO addRegexMappingRequestBodyDTO = jsonExtractor.parse(request.body());
        String mappingSourceString = addRegexMappingRequestBodyDTO.getSource();

        if ( mappingSourceString == null ) {
            return null;
        }

        // Check if user email matchs the pattern address@domain.com
        if ( mappingSourceString.matches("^[\\w-_\\.+]*[\\w-_\\.]\\@([\\w]+\\.)+[\\w]+[\\w]$") == false) {
            return null;
        }

        MappingSource mappingSource = MappingSource.parse(mappingSourceString);
        return mappingSource;
    }

    public String toRegex(Request request) throws Exception {
        AddRegexMappingRequestBodyDTO addRegexMappingRequestBodyDTO = jsonExtractor.parse(request.body());
        String regex = addRegexMappingRequestBodyDTO.getRegex();
        if ( regex == null )
        {
            return null;
        }
        return regex;
    }
}
