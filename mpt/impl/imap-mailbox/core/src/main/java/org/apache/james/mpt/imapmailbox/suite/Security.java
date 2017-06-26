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

package org.apache.james.mpt.imapmailbox.suite;

import org.apache.james.mpt.api.ImapHostSystem;
import org.apache.james.mpt.imapmailbox.ImapTestConstants;
import org.apache.james.mpt.imapmailbox.suite.base.LocaleParametrizedTest;
import org.apache.james.mpt.script.SimpleScriptedTestProtocol;
import org.junit.Before;
import org.junit.Test;

public abstract class Security extends LocaleParametrizedTest implements ImapTestConstants {

    protected abstract ImapHostSystem createImapHostSystem();
    
    private ImapHostSystem system;
    private SimpleScriptedTestProtocol simpleScriptedTestProtocol;

    @Before
    public void setUp() throws Exception {
        system = createImapHostSystem();
        simpleScriptedTestProtocol = new SimpleScriptedTestProtocol("/org/apache/james/imap/scripts/", system)
                .withUser(USER, PASSWORD)
                .withLocale(locale);
    }

    @Test
    public void accessingOtherPeopleNamespaceShouldBeDenied() throws Exception {
        simpleScriptedTestProtocol.run("SharedMailbox");
    }

    @Test
    public void testLoginThreeStrikes() throws Exception {
        simpleScriptedTestProtocol.run("LoginThreeStrikes");
    }

    @Test
    public void testBadTag() throws Exception {
        simpleScriptedTestProtocol.run("BadTag");
    }

    @Test
    public void testNoTag() throws Exception {
        simpleScriptedTestProtocol.run("NoTag");
    }


    @Test
    public void testIllegalTag() throws Exception {
        simpleScriptedTestProtocol.run("IllegalTag");
    }

    @Test
    public void testJustTag() throws Exception {
        simpleScriptedTestProtocol.run("JustTag");
    }

    @Test
    public void testNoCommand() throws Exception {
        simpleScriptedTestProtocol.run("NoCommand");
    }

    @Test
    public void testBogusCommand() throws Exception {
        simpleScriptedTestProtocol.run("BogusCommand");
    }

}
