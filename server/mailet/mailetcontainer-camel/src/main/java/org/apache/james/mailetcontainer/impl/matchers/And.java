package org.apache.james.mailetcontainer.impl.matchers;

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

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.apache.mailet.MailAddress;
import org.apache.mailet.Mail;
import javax.mail.MessagingException;
import org.apache.mailet.Matcher;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

/**
 * This matcher performs And conjunction between the two recipients
 */
public class And extends GenericCompositeMatcher {

    /**
     * This is the And CompositeMatcher - consider it to be an intersection of
     * the results. If any match returns an empty recipient result the matching
     * is short-circuited.
     * 
     * @return Collection of Recipient from the And composition results of the
     *         child Matchers.
     */
    public Collection<MailAddress> match(final Mail mail) throws MessagingException {
        List<Set<MailAddress>> individualMatchedResults = performMatchOnMatchers(mail);
        return computeIntersection(individualMatchedResults);
    }

    private Collection<MailAddress> computeIntersection(List<Set<MailAddress>> individualMatchedResults) {
        if (individualMatchedResults.size() == 0) {
            return null;
        }
        if (individualMatchedResults.size() == 1) {
            return individualMatchedResults.get(0);
        }
        Set<MailAddress> temporaryResult = ImmutableSet.copyOf(individualMatchedResults.get(0));
        List<Set<MailAddress>> followingResults = individualMatchedResults.subList(1, individualMatchedResults.size());
        for(Set<MailAddress> matchedAddresses: followingResults) {
            temporaryResult = Sets.intersection(temporaryResult, matchedAddresses);
        }
        return temporaryResult;
    }

    private List<Set<MailAddress>> performMatchOnMatchers(Mail mail) throws MessagingException {
        ImmutableList.Builder<Set<MailAddress>> builder = ImmutableList.builder();
        for (Matcher matcher : getMatchers()) {
            Collection<MailAddress> matchedMailAddress = matcher.match(mail);
            if (matchedMailAddress != null && !matchedMailAddress.isEmpty()) {
                builder.add(ImmutableSet.copyOf(matchedMailAddress));
            } else {
                return ImmutableList.of();
            }
        }
        return builder.build();
    }

}
