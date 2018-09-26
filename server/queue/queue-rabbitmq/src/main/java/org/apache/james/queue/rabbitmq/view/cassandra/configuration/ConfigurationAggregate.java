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

package org.apache.james.queue.rabbitmq.view.cassandra.configuration;

import java.util.List;
import java.util.Optional;

import org.apache.james.eventsourcing.AggregateId;
import org.apache.james.eventsourcing.Event;
import org.apache.james.eventsourcing.eventstore.History;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

class ConfigurationAggregate {

    private static class State {

        private static State initial() {
            return new State(Optional.empty());
        }

        private Optional<CassandraMailQueueViewConfiguration> maybeConfiguration;

        State(Optional<CassandraMailQueueViewConfiguration> maybeConfiguration) {
            Preconditions.checkNotNull(maybeConfiguration);
            this.maybeConfiguration = maybeConfiguration;
        }

        void set(CassandraMailQueueViewConfiguration configuration) {
            validateConfiguration(configuration);

            maybeConfiguration = Optional.of(configuration);
        }

        private void validateConfiguration(CassandraMailQueueViewConfiguration configurationUpdate) {
            Preconditions.checkNotNull(configurationUpdate);

            maybeConfiguration.ifPresent(currentConfiguration -> {
                Preconditions.checkArgument(!currentConfiguration.equals(configurationUpdate),
                    "new configuration need to be different from the current configuration: " + currentConfiguration.toString());

                Preconditions.checkArgument(configurationUpdate.getBucketCount() >= currentConfiguration.getBucketCount(),
                    "can not set 'bucketCount'(" + configurationUpdate.getBucketCount() + ") to be less than the current one: "
                        + currentConfiguration.getBucketCount());

                long updateSliceWindowInSecond = configurationUpdate.getSliceWindow().getSeconds();
                long currentSliceWindowInSecond = currentConfiguration.getSliceWindow().getSeconds();
                Preconditions.checkArgument(
                    updateSliceWindowInSecond <= currentSliceWindowInSecond
                        && currentSliceWindowInSecond % updateSliceWindowInSecond == 0,
                    "update 'sliceWindow'(" + configurationUpdate.getSliceWindow() + ") have to be less than and divisible by the current one: "
                        + currentConfiguration.getSliceWindow());
            });
        }
    }

    static ConfigurationAggregate load(History history) {
        return new ConfigurationAggregate(history);
    }

    private static final String CONFIGURATION_AGGREGATE_KEY = "CassandraMailQueueViewConfiguration";
    static final AggregateId CONFIGURATION_AGGREGATE_ID = () -> CONFIGURATION_AGGREGATE_KEY;

    private final History history;
    private final State state;

    ConfigurationAggregate(History history) {
        this.history = history;
        this.state = State.initial();

        history.getEvents().forEach(this::apply);
    }

    List<? extends Event> applyConfiguration(CassandraMailQueueViewConfiguration configuration) {
        ConfigurationEdited newEvent = new ConfigurationEdited(
            history.getNextEventId(),
            configuration);
        apply(newEvent);

        return ImmutableList.of(newEvent);
    }

    Optional<CassandraMailQueueViewConfiguration> getCurrentConfiguration() {
        return state.maybeConfiguration;
    }

    private void apply(Event event) {
        if (event instanceof ConfigurationEdited) {
            state.set(((ConfigurationEdited) event).getConfiguration());
        }
    }
}
