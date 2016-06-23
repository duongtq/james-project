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

package org.apache.james.webadmin.validation;

import com.google.common.base.CharMatcher;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

public class MailboxName {

    public static final CharMatcher INVALID_CHAR_MATCHER = CharMatcher.anyOf("%*&#");
    private final String mailboxName;

    public MailboxName(String mailboxName) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(mailboxName));
        Preconditions.checkArgument(INVALID_CHAR_MATCHER.matchesNoneOf(mailboxName));
        this.mailboxName = mailboxName;
    }

    public String asString() {
        return mailboxName;
    }
}
