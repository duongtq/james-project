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

import static org.apache.james.webadmin.Constants.SEPARATOR;
import static spark.Spark.halt;

import javax.inject.Inject;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.MessageManager;
import org.apache.james.mailbox.exception.MailboxException;
import org.apache.james.webadmin.Constants;
import org.apache.james.webadmin.Routes;
import org.apache.james.webadmin.service.ImportEmlService;
import org.apache.james.webadmin.utils.ErrorResponder;
import org.eclipse.jetty.http.HttpStatus;

import com.google.common.annotations.VisibleForTesting;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import spark.HaltException;
import spark.Request;
import spark.Response;
import spark.Service;

@Api(tags = "EML")
@Path(ImportEmlRoutes.BASE_PATH)
@Produces(Constants.JSON_CONTENT_TYPE)
public class ImportEmlRoutes implements Routes {

    public static final String MAILBOX_ID = ":mailboxId";
    static final String BASE_PATH = "/mailboxes";
    static final String MESSAGES = "messages";
    static final String IMPORT_EML_PATH = BASE_PATH + SEPARATOR + MAILBOX_ID + SEPARATOR + MESSAGES;
    static final int MAXIMUM_SIZE_IN_BYTE = 2684354;

    private ImportEmlService importEmlService;

    @Inject
    @VisibleForTesting
    ImportEmlRoutes(ImportEmlService importEmlService) {
        this.importEmlService = importEmlService;
    }

    @Override
    public String getBasePath() {
        return BASE_PATH;
    }

    @Override
    public void define(Service service) {
        service.post(IMPORT_EML_PATH, this::importEmlFileToMailbox);
    }

    @POST
    @Path(IMPORT_EML_PATH)
    @ApiOperation(value = "import an EML file and save it to a mailbox")
    @ApiResponses(value = {
        @ApiResponse(code = HttpStatus.NO_CONTENT_204, message = "EML file successfully imported"),
        @ApiResponse(code = HttpStatus.BAD_REQUEST_400, message = "Invalid parameter")
    })
    public HaltException importEmlFileToMailbox(Request request, Response response) throws Exception {
        final byte[] fileSize = request.body().getBytes();
        if (fileSize.length > MAXIMUM_SIZE_IN_BYTE) {
            throw ErrorResponder.builder()
                .statusCode(HttpStatus.PAYLOAD_TOO_LARGE_413)
                .type(ErrorResponder.ErrorType.WRONG_STATE)
                .message("File size exceed the limit (2.56MB)")
                .haltError();
        }

        MailboxSession session = getMailboxSessionFromEmlService();
        MessageManager mailbox = getMailboxFromEmlService(request);
        mailbox.appendMessage(MessageManager.AppendCommand.builder()
                .recent()
                .build(request.body()), session);
        return halt(HttpStatus.NO_CONTENT_204);
    }

    private MailboxSession getMailboxSessionFromEmlService() {
        try {
            return importEmlService.getMailboxSession();
        } catch (MailboxException e) {
             throw ErrorResponder.builder()
                .statusCode(HttpStatus.NOT_FOUND_404)
                .type(ErrorResponder.ErrorType.NOT_FOUND)
                .message("Unable to retrieve mailbox.")
                .haltError();
        }
    }

    private MessageManager getMailboxFromEmlService(Request request) {
        try {
            return importEmlService.retrieveMailbox(request.params(MAILBOX_ID));
        } catch (MailboxException e) {
             throw ErrorResponder.builder()
                .statusCode(HttpStatus.NOT_FOUND_404)
                .type(ErrorResponder.ErrorType.NOT_FOUND)
                .message("Unable to retrieve mailbox.")
                .haltError();
        }
    }
}