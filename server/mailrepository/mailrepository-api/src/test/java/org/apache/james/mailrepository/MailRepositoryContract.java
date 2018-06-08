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

package org.apache.james.mailrepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.apache.james.core.MailAddress;
import org.apache.james.core.builder.MimeMessageBuilder;
import org.apache.james.mailrepository.api.MailKey;
import org.apache.james.mailrepository.api.MailRepository;
import org.apache.james.server.core.MailImpl;
import org.apache.james.util.concurrency.ConcurrentTestRunner;
import org.apache.james.utils.DiscreteDistribution;
import org.apache.james.utils.DiscreteDistribution.DistributionEntry;
import org.apache.mailet.Mail;
import org.apache.mailet.PerRecipientHeaders;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import com.github.fge.lambdas.runnable.ThrowingRunnable;
import com.github.steveash.guavate.Guavate;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.hash.Hashing;

public interface MailRepositoryContract {

    String TEST_ATTRIBUTE = "testAttribute";
    MailKey MAIL_1 = new MailKey("mail1");
    MailKey MAIL_2 = new MailKey("mail2");
    MailKey UNKNOWN_KEY = new MailKey("random");

    default MailImpl createMail(MailKey key) throws MessagingException {
        return createMail(key, "original body");
    }

    default MailImpl createMail(MailKey key, String body) throws MessagingException {
        MimeMessage mailContent = generateMailContent(body);
        List<MailAddress> recipients = ImmutableList
            .of(new MailAddress("rec1@domain.com"),
                new MailAddress("rec2@domain.com"));
        MailAddress sender = new MailAddress("sender@domain.com");
        MailImpl mail = new MailImpl(key.getValue(), sender, recipients, mailContent);
        mail.setAttribute(TEST_ATTRIBUTE, "testValue");
        return mail;
    }


    default MimeMessage generateMailContent(String body) throws MessagingException {
        return MimeMessageBuilder.mimeMessageBuilder()
            .setSubject("test")
            .setText(body)
            .build();
    }

    default void checkMailEquality(Mail actual, Mail expected) {
        assertAll(
            () -> assertThat(actual.getMessage().getContent()).isEqualTo(expected.getMessage().getContent()),
            () -> assertThat(actual.getMessageSize()).isEqualTo(expected.getMessageSize()),
            () -> assertThat(actual.getName()).isEqualTo(expected.getName()),
            () -> assertThat(actual.getState()).isEqualTo(expected.getState()),
            () -> assertThat(actual.getAttribute(TEST_ATTRIBUTE)).isEqualTo(expected.getAttribute(TEST_ATTRIBUTE)),
            () -> assertThat(actual.getErrorMessage()).isEqualTo(expected.getErrorMessage()),
            () -> assertThat(actual.getRemoteHost()).isEqualTo(expected.getRemoteHost()),
            () -> assertThat(actual.getRemoteAddr()).isEqualTo(expected.getRemoteAddr()),
            () -> assertThat(actual.getLastUpdated()).isEqualTo(expected.getLastUpdated()),
            () -> assertThat(actual.getPerRecipientSpecificHeaders()).isEqualTo(expected.getPerRecipientSpecificHeaders())
        );
    }

    MailRepository retrieveRepository() throws Exception;

    @Test
    default void sizeShouldReturnZeroWhenEmpty() throws Exception {
        MailRepository testee = retrieveRepository();
        assertThat(testee.size()).isEqualTo(0L);
    }

    @Test
    default void sizeShouldReturnMailCount() throws Exception {
        MailRepository testee = retrieveRepository();

        testee.store(createMail(MAIL_1));
        testee.store(createMail(MAIL_2));

        assertThat(testee.size()).isEqualTo(2L);
    }

    @Test
    default void sizeShouldBeIncrementedByOneWhenDuplicates() throws Exception {
        MailRepository testee = retrieveRepository();

        testee.store(createMail(MAIL_1));
        testee.store(createMail(MAIL_1));

        assertThat(testee.size()).isEqualTo(1L);
    }

    @Test
    default void sizeShouldBeDecrementedByRemove() throws Exception {
        MailRepository testee = retrieveRepository();

        testee.store(createMail(MAIL_1));
        testee.remove(MAIL_1);

        assertThat(testee.size()).isEqualTo(0L);
    }

    @Test
    default void storeRegularMailShouldNotFail() throws Exception {
        MailRepository testee = retrieveRepository();
        Mail mail = createMail(MAIL_1);

        testee.store(mail);
    }

    @Test
    default void storeBigMailShouldNotFail() throws Exception {
        MailRepository testee = retrieveRepository();
        String bigString = Strings.repeat("my mail is big 🐋", 1_000_000);
        Mail mail = createMail(MAIL_1, bigString);

        testee.store(mail);
    }

    @Test
    default void retrieveShouldGetStoredMail() throws Exception {
        MailRepository testee = retrieveRepository();
        Mail mail = createMail(MAIL_1);

        testee.store(mail);

        assertThat(testee.retrieve(MAIL_1)).satisfies(actual -> checkMailEquality(actual, mail));
    }

    @Test
    default void removeAllShouldRemoveStoredMails() throws Exception {
        MailRepository testee = retrieveRepository();
        testee.store(createMail(MAIL_1));

        testee.removeAll();

        assertThat(testee.size()).isEqualTo(0L);
    }

    @Test
    default void retrieveShouldReturnNullAfterRemoveAll() throws Exception {
        MailRepository testee = retrieveRepository();
        testee.store(createMail(MAIL_1));

        testee.removeAll();

        assertThat(testee.retrieve(MAIL_1)).isNull();
    }

    @Test
    default void removeAllShouldBeIdempotent() throws Exception {
        MailRepository testee = retrieveRepository();
        testee.store(createMail(MAIL_1));

        testee.removeAll();
        testee.removeAll();

        assertThat(testee.size()).isEqualTo(0L);
    }

    @Test
    default void removeAllShouldNotFailWhenEmpty() throws Exception {
        MailRepository testee = retrieveRepository();

        testee.removeAll();
    }

    @Test
    default void retrieveShouldGetStoredEmojiMail() throws Exception {
        MailRepository testee = retrieveRepository();
        Mail mail = createMail(MAIL_1, "my content contains 🐋");

        testee.store(mail);

        assertThat(testee.retrieve(MAIL_1).getMessage().getContent()).isEqualTo("my content contains 🐋");
    }

    @Test
    default void retrieveBigMailShouldHaveSameHash() throws Exception {
        MailRepository testee = retrieveRepository();
        String bigString = Strings.repeat("my mail is big 🐋", 1_000_000);
        Mail mail = createMail(MAIL_1, bigString);
        testee.store(mail);

        Mail actual = testee.retrieve(MAIL_1);

        assertThat(Hashing.sha256().hashString((String)actual.getMessage().getContent(), StandardCharsets.UTF_8))
            .isEqualTo(Hashing.sha256().hashString(bigString, StandardCharsets.UTF_8));
    }


    @Test
    default void retrieveShouldReturnAllMailProperties() throws Exception {
        MailRepository testee = retrieveRepository();
        MailImpl mail = createMail(MAIL_1);
        mail.setErrorMessage("Error message");
        mail.setRemoteAddr("172.5.2.3");
        mail.setRemoteHost("smtp@domain.com");
        mail.setLastUpdated(new Date());
        mail.addSpecificHeaderForRecipient(PerRecipientHeaders.Header.builder()
            .name("name")
            .value("value")
            .build(),
            new MailAddress("bob@domain.com"));

        testee.store(mail);

        assertThat(testee.retrieve(MAIL_1)).satisfies(actual -> checkMailEquality(actual, mail));
    }

    @Test
    default void newlyCreatedRepositoryShouldNotContainAnyMail() throws Exception {
        MailRepository testee = retrieveRepository();

        assertThat(testee.list()).isEmpty();
    }

    @Test
    default void retrievingUnknownMailShouldReturnNull() throws Exception {
        MailRepository testee = retrieveRepository();

        assertThat(testee.retrieve(UNKNOWN_KEY)).isNull();
    }

    @Test
    default void removingUnknownMailShouldHaveNoEffect() throws Exception {
        MailRepository testee = retrieveRepository();

        testee.remove(UNKNOWN_KEY);
    }

    @Test
    default void retrieveShouldReturnNullWhenKeyWasRemoved() throws Exception {
        MailRepository testee = retrieveRepository();
        testee.store(createMail(MAIL_1));

        testee.remove(MAIL_1);

        assertThat(retrieveRepository().list()).doesNotContain(MAIL_1);
        assertThat(retrieveRepository().retrieve(MAIL_1)).isNull();
    }

    @Test
    default void removeShouldnotAffectUnrelatedMails() throws Exception {
        MailRepository testee = retrieveRepository();
        testee.store(createMail(MAIL_1));
        testee.store(createMail(MAIL_2));

        testee.remove(MAIL_1);

        assertThat(retrieveRepository().list()).contains(MAIL_2);
    }

    @Test
    default void removedMailsShouldNotBeListed() throws Exception {
        MailRepository testee = retrieveRepository();

        MailKey key3 = new MailKey("mail3");
        Mail mail1 = createMail(MAIL_1);
        Mail mail2 = createMail(MAIL_2);
        Mail mail3 = createMail(key3);
        retrieveRepository().store(mail1);
        retrieveRepository().store(mail2);
        retrieveRepository().store(mail3);

        testee.remove(ImmutableList.of(mail1, mail3));

        assertThat(retrieveRepository().list())
            .contains(MAIL_2)
            .doesNotContain(MAIL_1, key3);
    }

    @Test
    default void removedMailShouldNotBeListed() throws Exception {
        MailRepository testee = retrieveRepository();

        MailKey key3 = new MailKey("mail3");
        Mail mail1 = createMail(MAIL_1);
        Mail mail2 = createMail(MAIL_2);
        Mail mail3 = createMail(key3);
        retrieveRepository().store(mail1);
        retrieveRepository().store(mail2);
        retrieveRepository().store(mail3);

        testee.remove(mail2);

        assertThat(retrieveRepository().list())
            .contains(MAIL_1, key3)
            .doesNotContain(MAIL_2);
    }

    @Test
    default void removeShouldHaveNoEffectForUnknownMails() throws Exception {
        MailRepository testee = retrieveRepository();

        testee.remove(ImmutableList.of(createMail(UNKNOWN_KEY)));

        assertThat(retrieveRepository().list()).isEmpty();
    }

    @Test
    default void removeShouldHaveNoEffectForUnknownMail() throws Exception {
        MailRepository testee = retrieveRepository();

        testee.remove(createMail(UNKNOWN_KEY));

        assertThat(retrieveRepository().list()).isEmpty();
    }

    @Test
    default void listShouldReturnStoredMailsKeys() throws Exception {
        MailRepository testee = retrieveRepository();
        testee.store(createMail(MAIL_1));

        testee.store(createMail(MAIL_2));

        assertThat(testee.list()).containsOnly(MAIL_1, MAIL_2);
    }

    @Test
    default void storingMessageWithSameKeyTwiceShouldUpdateMessageContent() throws Exception {
        MailRepository testee = retrieveRepository();
        testee.store(createMail(MAIL_1));

        Mail updatedMail = createMail(MAIL_1, "modified content");
        testee.store(updatedMail);

        assertThat(testee.list()).hasSize(1);
        assertThat(testee.retrieve(MAIL_1)).satisfies(actual -> checkMailEquality(actual, updatedMail));
    }

    @Test
    default void storingMessageWithSameKeyTwiceShouldUpdateMessageAttributes() throws Exception {
        MailRepository testee = retrieveRepository();
        Mail mail = createMail(MAIL_1);
        testee.store(mail);

        mail.setAttribute(TEST_ATTRIBUTE, "newValue");
        testee.store(mail);

        assertThat(testee.list()).hasSize(1);
        assertThat(testee.retrieve(MAIL_1)).satisfies(actual -> checkMailEquality(actual, mail));
    }

    @RepeatedTest(100)
    default void storingAndRemovingMessagesConcurrentlyShouldLeadToConsistentResult() throws Exception {
        MailRepository testee = retrieveRepository();
        int nbKeys = 20;
        int nbIterations = 10;
        int threadCount = 10;
        ConcurrentHashMap.KeySetView<MailKey, Boolean> expectedResult = ConcurrentHashMap.newKeySet();
        List<Object> locks = IntStream.range(0, 10)
            .boxed()
            .collect(Guavate.toImmutableList());

        Random random = new Random();
        ThrowingRunnable add = () -> {
            int keyIndex = computeKeyIndex(nbKeys, random.nextInt());
            MailKey key =  computeKey(keyIndex);
            synchronized (locks.get(keyIndex)) {
                testee.store(createMail(key));
                expectedResult.add(key);
            }
        };

        ThrowingRunnable remove = () -> {
            int keyIndex = computeKeyIndex(nbKeys, random.nextInt());
            MailKey key =  computeKey(keyIndex);
            synchronized (locks.get(keyIndex)) {
                testee.remove(key);
                expectedResult.remove(key);
            }
        };

        DiscreteDistribution<ThrowingRunnable> distribution = DiscreteDistribution.create(
            ImmutableList.of(
                new DistributionEntry<>(add, 0.25),
                new DistributionEntry<>(remove, 0.75)));

        new ConcurrentTestRunner(threadCount, nbIterations,
            (a, b) -> distribution.sample().run())
            .run()
            .awaitTermination(1, TimeUnit.MINUTES);

        assertThat(testee.list()).containsOnlyElementsOf(expectedResult);
    }

    default MailKey computeKey(int keyIndex) {
        return new MailKey("mail" + keyIndex);
    }

    default int computeKeyIndex(int nbKeys, Integer i) {
        return i % nbKeys;
    }

}
