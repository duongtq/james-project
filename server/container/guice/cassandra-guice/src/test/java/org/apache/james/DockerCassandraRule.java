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

package org.apache.james;

import org.apache.james.backends.cassandra.init.configuration.ClusterConfiguration;
import org.apache.james.server.CassandraCleanupProbe;
import org.apache.james.util.Host;
import org.apache.james.utils.GuiceProbe;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.testcontainers.containers.GenericContainer;

import com.google.inject.Module;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.util.Modules;


public class DockerCassandraRule implements GuiceModuleTestRule {

    public static final String TESTING = "testing";
    private org.apache.james.backends.cassandra.DockerCassandraRule cassandraContainer = new org.apache.james.backends.cassandra.DockerCassandraRule();

    @Override
    public Statement apply(Statement base, Description description) {
        return cassandraContainer.apply(base, description);
    }

    @Override
    public void await() {
    }

    @Override
    public Module getModule() {
        return Modules.combine(binder -> binder.bind(ClusterConfiguration.class)
            .toInstance(ClusterConfiguration.builder()
                .host(cassandraContainer.getHost())
                .keyspace(TESTING)
                .replicationFactor(1)
                .maxRetry(20)
                .minDelay(5000)
                .build()),
            binder -> Multibinder.newSetBinder(binder, GuiceProbe.class)
                .addBinding()
                .to(CassandraCleanupProbe.class));
    }

    public Host getHost() {
        return cassandraContainer.getHost();
    }

    public Integer getMappedPort(int originalPort) {
        return getHost().getPort();
    }

    public void start() {
        cassandraContainer.start();
    }

    public void stop() {
        cassandraContainer.stop();
    }

    public GenericContainer<?> getRawContainer() {
        return cassandraContainer.getRawContainer();
    }

    public void pause() {
        cassandraContainer.pause();
    }

    public void unpause() {
        cassandraContainer.unpause();
    }

}
