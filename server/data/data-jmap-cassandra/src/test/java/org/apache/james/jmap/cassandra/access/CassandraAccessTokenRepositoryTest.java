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

package org.apache.james.jmap.cassandra.access;

import org.apache.james.backends.cassandra.CassandraCluster;
import org.apache.james.backends.cassandra.DockerCassandraRule;
import org.apache.james.jmap.api.access.AccessTokenRepository;
import org.apache.james.jmap.api.access.AccessTokenRepositoryTest;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;

public class CassandraAccessTokenRepositoryTest extends AccessTokenRepositoryTest {
    

    @ClassRule public static DockerCassandraRule cassandraServer = new DockerCassandraRule();
    
    private CassandraCluster cassandra;

    @Override
    @Before
    public void setUp() throws Exception {
        cassandra = CassandraCluster.create(new CassandraAccessModule(), cassandraServer.getIp(), cassandraServer.getBindingPort());
        super.setUp();
    }
    
    @After
    public void tearDown() throws Exception {
        cassandra.close();
    }
    
    @Override
    protected AccessTokenRepository provideAccessTokenRepository() {
        return new CassandraAccessTokenRepository(
            new CassandraAccessTokenDAO(cassandra.getConf(), AccessTokenRepositoryTest.TTL_IN_MS));
    }

}