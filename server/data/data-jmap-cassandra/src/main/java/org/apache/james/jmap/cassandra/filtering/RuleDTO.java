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

package org.apache.james.jmap.cassandra.filtering;

import java.util.List;
import java.util.Objects;

import org.apache.james.jmap.api.filtering.Rule;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

public class RuleDTO {

    public static ImmutableList<Rule> toRules(List<RuleDTO> ruleDTOList) {
        Preconditions.checkNotNull(ruleDTOList);
        return ruleDTOList.stream()
                .map(RuleDTO::toRule)
                .collect(ImmutableList.toImmutableList());
    }

    public static ImmutableList<RuleDTO> from(List<Rule> rules) {
        Preconditions.checkNotNull(rules);
        return rules.stream()
            .map(RuleDTO::from)
            .collect(ImmutableList.toImmutableList());
    }

    public static RuleDTO from(Rule rule) {
        return new RuleDTO(rule.getId().asString(),
                rule.getName(),
                rule.getCondition().getField().asString(),
                rule.getCondition().getComparator().asString(),
                rule.getCondition().getValue(),
                rule.getAction().getAppendInMailboxes().getMailboxIds());
    }

    private final String id;
    private final String name;
    private final String field;
    private final String comparator;
    private final String value;
    private final List<String> mailboxIds;

    @JsonCreator
    public RuleDTO(@JsonProperty("id") String id,
                   @JsonProperty("name") String name,
                   @JsonProperty("field") String field,
                   @JsonProperty("comparator") String comparator,
                   @JsonProperty("value") String value,
                   @JsonProperty("mailboxIds") List<String> mailboxIds) {
        this.name = name;
        this.field = field;
        this.comparator = comparator;
        this.value = value;
        this.mailboxIds = ImmutableList.copyOf(mailboxIds);
        Preconditions.checkNotNull(id);

        this.id = id;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getField() {
        return field;
    }

    public String getComparator() {
        return comparator;
    }

    public String getValue() {
        return value;
    }

    public List<String> getMailboxIds() {
        return mailboxIds;
    }

    public Rule toRule() {
        return Rule.builder()
            .id(Rule.Id.of(id))
            .name(name)
            .condition(Rule.Condition.of(
                Rule.Condition.Field.valueOf(field),
                Rule.Condition.Comparator.of(comparator),
                value))
            .name(name)
            .action(Rule.Action.of(Rule.AppendInMailboxes.ofMailboxIds(mailboxIds)))
            .build();
    }

    @Override
    public final boolean equals(Object o) {
        if (o instanceof RuleDTO) {
            RuleDTO ruleDTO = (RuleDTO) o;

            return Objects.equals(this.id, ruleDTO.id)
                   && Objects.equals(this.name, ruleDTO.name)
                   && Objects.equals(this.field, ruleDTO.field)
                   && Objects.equals(this.comparator, ruleDTO.comparator)
                   && Objects.equals(this.value, ruleDTO.value)
                   && Objects.equals(this.mailboxIds, ruleDTO.mailboxIds);
        }
        return false;
    }

    @Override
    public final int hashCode() {
        return Objects.hash(id, name, field, comparator, value, mailboxIds);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("id", id)
            .toString();
    }
}
