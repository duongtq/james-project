package org.apache.james.mpt.imapmailbox.lucenesearch;

import org.apache.james.mpt.api.ImapHostSystem;
import org.apache.james.mpt.imapmailbox.suite.MailboxWithLongNameError;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;

import com.google.inject.Guice;
import com.google.inject.Injector;

@Ignore("MAILBOX-300 JPA should fail gracefully when too long mailbox name")
public class LuceneMailboxWithLongNameError extends MailboxWithLongNameError {

    private ImapHostSystem system;

    @Before
    public void setUp() throws Exception {
        Injector injector = Guice.createInjector(new LuceneSearchMailboxTestModule());
        system = injector.getInstance(ImapHostSystem.class);
        system.beforeTest();
        super.setUp();
    }
    
    @Override
    protected ImapHostSystem createImapHostSystem() {
        return system;
    }

    @After
    public void tearDown() throws Exception {
        system.afterTest();
    }
    
}
