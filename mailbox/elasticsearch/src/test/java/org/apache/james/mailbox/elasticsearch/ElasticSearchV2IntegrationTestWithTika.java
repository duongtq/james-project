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

package org.apache.james.mailbox.elasticsearch;

import java.net.URISyntaxException;
import java.time.ZoneId;

import org.apache.james.mailbox.elasticsearch.json.MessageToElasticSearchJson;
import org.apache.james.mailbox.elasticsearch.json.MessageToElasticSearchJsonV1;
import org.apache.james.mailbox.elasticsearch.query.CriterionConverter;
import org.apache.james.mailbox.elasticsearch.query.QueryConverter;
import org.apache.james.mailbox.elasticsearch.search.ElasticSearchSearcher;
import org.apache.james.mailbox.elasticsearch.search.ElasticSearchSearcherV1;
import org.apache.james.mailbox.extractor.TextExtractor;
import org.apache.james.mailbox.model.MailboxId;
import org.apache.james.mailbox.model.MessageId;
import org.apache.james.mailbox.tika.TikaConfiguration;
import org.apache.james.mailbox.tika.TikaContainer;
import org.apache.james.mailbox.tika.TikaHttpClientImpl;
import org.apache.james.mailbox.tika.TikaTextExtractor;
import org.elasticsearch.client.Client;
import org.junit.Assume;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;
import org.testcontainers.shaded.com.google.common.base.Throwables;

public class ElasticSearchV2IntegrationTestWithTika extends  ElasticSearchIntegrationTest {

    private static final int SEARCH_SIZE = 1;

    @ClassRule
    public static TikaContainer tika = new TikaContainer();

    @Override
    protected TextExtractor getTextExtractor() {
        try {
            return new TikaTextExtractor(new TikaHttpClientImpl(TikaConfiguration.builder()
                .host(tika.getIp())
                .port(tika.getPort())
                .timeoutInMillis(tika.getTimeoutInMillis())
                .build()));
        } catch (URISyntaxException e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    protected MailboxMappingFactory provideMappingFactory() {
        return new MailboxMappingFactoryV2();
    }

    @Override
    protected ElasticSearchSearcher provideSearcher(Client client,
                                                    MailboxId.Factory mailboxIdFactory,
                                                    MessageId.Factory messageIdFactory) {
        return new ElasticSearchSearcherV1(client, new QueryConverter(new CriterionConverter()), SEARCH_SIZE,
            mailboxIdFactory, messageIdFactory,
            MailboxElasticSearchConstants.DEFAULT_MAILBOX_READ_ALIAS,
            MailboxElasticSearchConstants.MESSAGE_TYPE);
    }

    @Override
    protected MessageToElasticSearchJson provideMessageToElasticSearchJson(TextExtractor textExtractor,
                                                                           ZoneId zoneId) {
        return new MessageToElasticSearchJsonV1(textExtractor, zoneId, IndexAttachments.YES);
    }


    @Override
    @Test
    @Ignore("See MAILBOX-314 and upgrade to ElasticSearch schema version 2")
    public void sortOnFromShouldWork() throws Exception {
        Assume.assumeTrue(false);
    }

    @Override
    @Test
    @Ignore("See MAILBOX-314 and upgrade to ElasticSearch schema version 2")
    public void searchWithTextShouldReturnMailsWhenCcMatches() throws Exception {
        Assume.assumeTrue(false);
    }

    @Override
    @Test
    @Ignore("See MAILBOX-314 and upgrade to ElasticSearch schema version 2")
    public void addressShouldReturnUidHavingRightExpeditorWhenFromIsSpecifiedWithDomainPartOfEmail() throws Exception {
        Assume.assumeTrue(false);
    }

    @Override
    @Test
    @Ignore("See MAILBOX-314 and upgrade to ElasticSearch schema version 2")
    public void searchShouldBeExactOnEmail() {
        Assume.assumeTrue(false);
    }
}
