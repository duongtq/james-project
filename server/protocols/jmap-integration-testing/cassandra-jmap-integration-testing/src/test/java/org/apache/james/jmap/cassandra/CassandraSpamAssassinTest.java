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
package org.apache.james.jmap.cassandra;

import org.apache.james.CassandraExtension;
import org.apache.james.EmbeddedElasticSearchExtension;
import org.apache.james.GuiceJamesServer;
import org.apache.james.JamesServerExtension;
import org.apache.james.jmap.SpamAssassinGuiceExtension;
import org.apache.james.jmap.methods.integration.SpamAssassinContract;
import org.apache.james.modules.CassandraJMAPTestModule;
import org.junit.jupiter.api.extension.RegisterExtension;

class CassandraSpamAssassinTest implements SpamAssassinContract {
    private static final SpamAssassinGuiceExtension SPAM_ASSASSIN_GUICE_EXTENSION = new SpamAssassinGuiceExtension();

    @RegisterExtension
    static JamesServerExtension testExtension = JamesServerExtension.builder()
        .extension(new EmbeddedElasticSearchExtension())
        .extension(new CassandraExtension())
        .extension(SPAM_ASSASSIN_GUICE_EXTENSION)
        .server(configuration -> GuiceJamesServer.forConfiguration(configuration)
            .combineWith(CassandraJMAPTestModule.DEFAULT))
        .build();

    @Override
    public void train(String username) throws Exception {
        SPAM_ASSASSIN_GUICE_EXTENSION.getBaseExtension().getSpamAssassin().train(username);
    }
}
