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

package org.apache.james.mailbox.store.quota;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Optional;

import org.apache.james.core.quota.QuotaCount;
import org.apache.james.core.quota.QuotaSize;
import org.apache.james.mailbox.model.QuotaRoot;
import org.junit.jupiter.api.Test;

public interface StoreCurrentQuotaManagerContract {
    QuotaRoot QUOTA_ROOT = QuotaRoot.quotaRoot("benwa", Optional.empty());
    
    StoreCurrentQuotaManager testee();

    @Test
    default void getCurrentStorageShouldReturnZeroByDefault() throws Exception {
        assertThat(testee().getCurrentStorage(QUOTA_ROOT)).isEqualTo(QuotaSize.size(0));
    }

    @Test
    default void increaseShouldWork() throws Exception {
        testee().increase(QUOTA_ROOT, 10, 100);

        assertThat(testee().getCurrentMessageCount(QUOTA_ROOT)).isEqualTo(QuotaCount.count(10));
        assertThat(testee().getCurrentStorage(QUOTA_ROOT)).isEqualTo(QuotaSize.size(100));
    }

    @Test
    default void decreaseShouldWork() throws Exception {
        testee().increase(QUOTA_ROOT, 20, 200);

        testee().decrease(QUOTA_ROOT, 10, 100);

        assertThat(testee().getCurrentMessageCount(QUOTA_ROOT)).isEqualTo(QuotaCount.count(10));
        assertThat(testee().getCurrentStorage(QUOTA_ROOT)).isEqualTo(QuotaSize.size(100));
    }

    @Test
    default void decreaseShouldNotFailWhenItLeadsToNegativeValues() throws Exception {
        testee().decrease(QUOTA_ROOT, 10, 100);

        assertThat(testee().getCurrentMessageCount(QUOTA_ROOT)).isEqualTo(QuotaCount.count(-10));
        assertThat(testee().getCurrentStorage(QUOTA_ROOT)).isEqualTo(QuotaSize.size(-100));
    }

    @Test
    default void increaseShouldThrowOnZeroCount() {
        assertThatThrownBy(() -> testee().increase(QUOTA_ROOT, 0, 5))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    default void increaseShouldThrowOnNegativeCount() {
        assertThatThrownBy(() -> testee().increase(QUOTA_ROOT, -1, 5))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    default void increaseShouldThrowOnZeroSize() {
        assertThatThrownBy(() -> testee().increase(QUOTA_ROOT, 5, 0))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    default void increaseShouldThrowOnNegativeSize() {
        assertThatThrownBy(() -> testee().increase(QUOTA_ROOT, 5, -1))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    default void decreaseShouldThrowOnZeroCount() {
        assertThatThrownBy(() -> testee().decrease(QUOTA_ROOT, 0, 5))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    default void decreaseShouldThrowOnNegativeCount() {
        assertThatThrownBy(() -> testee().decrease(QUOTA_ROOT, -1, 5))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    default void decreaseShouldThrowOnZeroSize() {
        assertThatThrownBy(() -> testee().decrease(QUOTA_ROOT, 5, 0))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    default void decreaseShouldThrowOnNegativeSize() {
        assertThatThrownBy(() -> testee().decrease(QUOTA_ROOT, 5, -1))
            .isInstanceOf(IllegalArgumentException.class);
    }
}