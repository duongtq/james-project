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

package org.apache.james.jmap.methods;

import java.util.Collection;
import java.util.Optional;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.apache.commons.lang.NotImplementedException;
import org.apache.james.jmap.json.FieldNamePropertyFilter;
import org.apache.james.jmap.model.ClientId;
import org.apache.james.jmap.model.GetMessagesRequest;
import org.apache.james.jmap.model.GetMessagesResponse;
import org.apache.james.jmap.model.MessageFactory;
import org.apache.james.jmap.model.MessageFactory.MetaDataWithContent;
import org.apache.james.jmap.model.MessageProperties;
import org.apache.james.jmap.model.MessageProperties.HeaderProperty;
import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.MessageIdManager;
import org.apache.james.mailbox.exception.MailboxException;
import org.apache.james.mailbox.model.FetchGroupImpl;
import org.apache.james.mailbox.model.MessageResult;
import org.apache.james.metrics.api.MetricFactory;
import org.apache.james.metrics.api.TimeMetric;

import com.fasterxml.jackson.databind.ser.PropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import com.github.fge.lambdas.Throwing;
import com.github.fge.lambdas.functions.ThrowingFunction;
import com.github.steveash.guavate.Guavate;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableSet;

public class GetMessagesMethod implements Method {

    public static final String HEADERS_FILTER = "headersFilter";
    private static final Method.Request.Name METHOD_NAME = Method.Request.name("getMessages");
    private static final Method.Response.Name RESPONSE_NAME = Method.Response.name("messages");
    private final MessageFactory messageFactory;
    private final MessageIdManager messageIdManager;
    private final MetricFactory metricFactory;

    @Inject
    @VisibleForTesting GetMessagesMethod(
            MessageFactory messageFactory,
            MessageIdManager messageIdManager,
            MetricFactory metricFactory) {
        this.messageFactory = messageFactory;
        this.messageIdManager = messageIdManager;
        this.metricFactory = metricFactory;
    }
    
    @Override
    public Method.Request.Name requestHandled() {
        return METHOD_NAME;
    }
    
    @Override
    public Class<? extends JmapRequest> requestType() {
        return GetMessagesRequest.class;
    }
    
    @Override
    public Stream<JmapResponse> process(JmapRequest request, ClientId clientId, MailboxSession mailboxSession) {
        Preconditions.checkNotNull(request);
        Preconditions.checkNotNull(mailboxSession);
        Preconditions.checkArgument(request instanceof GetMessagesRequest);

        GetMessagesRequest getMessagesRequest = (GetMessagesRequest) request;
        MessageProperties outputProperties = getMessagesRequest.getProperties().toOutputProperties();
        TimeMetric timeMetric = metricFactory.timer(METHOD_NAME.getName());
        Stream<JmapResponse> responses = Stream.of(JmapResponse.builder().clientId(clientId)
                            .response(getMessagesResponse(mailboxSession, getMessagesRequest))
                            .responseName(RESPONSE_NAME)
                            .properties(outputProperties.getOptionalMessageProperties())
                            .filterProvider(buildOptionalHeadersFilteringFilterProvider(outputProperties))
                            .build());
        timeMetric.elapseTimeInMs();
        return responses;
    }

    private Optional<SimpleFilterProvider> buildOptionalHeadersFilteringFilterProvider(MessageProperties properties) {
        return properties.getOptionalHeadersProperties()
            .map(this::buildHeadersPropertyFilter)
            .map(propertyFilter -> new SimpleFilterProvider()
                .addFilter(HEADERS_FILTER, propertyFilter));
    }
    
    private PropertyFilter buildHeadersPropertyFilter(ImmutableSet<HeaderProperty> headerProperties) {
        return new FieldNamePropertyFilter((fieldName) -> headerProperties.contains(HeaderProperty.fromFieldName(fieldName)));
    }

    private GetMessagesResponse getMessagesResponse(MailboxSession mailboxSession, GetMessagesRequest getMessagesRequest) {
        getMessagesRequest.getAccountId().ifPresent((input) -> notImplemented());

        try {
            return GetMessagesResponse.builder()
                .messages(
                    messageIdManager.getMessages(getMessagesRequest.getIds(), FetchGroupImpl.FULL_CONTENT, mailboxSession)
                        .stream()
                        .collect(Guavate.toImmutableListMultimap(MessageResult::getMessageId))
                        .asMap()
                        .values()
                        .stream()
                        .filter(collection -> !collection.isEmpty())
                        .map(Throwing.function(toMetaDataWithContent()).sneakyThrow())
                        .map(Throwing.function(messageFactory::fromMetaDataWithContent).sneakyThrow())
                        .collect(Guavate.toImmutableList()))
                .expectedMessageIds(getMessagesRequest.getIds())
                .build();
        } catch (MailboxException e) {
            throw Throwables.propagate(e);
        }
    }

    private ThrowingFunction<Collection<MessageResult>, MetaDataWithContent> toMetaDataWithContent() {
        return messageResults -> {
            MessageResult firstMessageResult = messageResults.iterator().next();
            return MetaDataWithContent.builderFromMessageResult(firstMessageResult)
                .messageId(firstMessageResult.getMessageId())
                .mailboxIds(messageResults.stream()
                    .map(MessageResult::getMailboxId)
                    .distinct()
                    .collect(Guavate.toImmutableList()))
                .build();
        };
    }

    private static void notImplemented() {
        throw new NotImplementedException();
    }
}