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

package org.apache.james.transport.mailets;

import org.apache.james.jdkim.DKIMVerifier;
import org.apache.james.jdkim.MockPublicKeyRecordRetriever;
import org.apache.james.jdkim.api.SignatureRecord;
import org.apache.james.jdkim.exceptions.FailException;
import org.apache.james.jdkim.exceptions.PermFailException;
import org.apache.mailet.Mail;
import org.apache.mailet.Mailet;
import org.apache.mailet.base.test.FakeMail;
import org.apache.mailet.base.test.FakeMailContext;
import org.apache.mailet.base.test.FakeMailetConfig;
import org.junit.Assert;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.junit.Test;

import javax.mail.Address;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMessage.RecipientType;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Properties;

public class DKIMSignTest {

    private static final String TESTING_PEM = "-----BEGIN RSA PRIVATE KEY-----\r\n" +
            "MIICXAIBAAKBgQDYDaYKXzwVYwqWbLhmuJ66aTAN8wmDR+rfHE8HfnkSOax0oIoT\r\n" +
            "M5zquZrTLo30870YMfYzxwfB6j/Nz3QdwrUD/t0YMYJiUKyWJnCKfZXHJBJ+yfRH\r\n" +
            "r7oW+UW3cVo9CG2bBfIxsInwYe175g9UjyntJpWueqdEIo1c2bhv9Mp66QIDAQAB\r\n" +
            "AoGBAI8XcwnZi0Sq5N89wF+gFNhnREFo3rsJDaCY8iqHdA5DDlnr3abb/yhipw0I\r\n" +
            "/1HlgC6fIG2oexXOXFWl+USgqRt1kTt9jXhVFExg8mNko2UelAwFtsl8CRjVcYQO\r\n" +
            "cedeH/WM/mXjg2wUqqZenBmlKlD6vNb70jFJeVaDJ/7n7j8BAkEA9NkH2D4Zgj/I\r\n" +
            "OAVYccZYH74+VgO0e7VkUjQk9wtJ2j6cGqJ6Pfj0roVIMUWzoBb8YfErR8l6JnVQ\r\n" +
            "bfy83gJeiQJBAOHk3ow7JjAn8XuOyZx24KcTaYWKUkAQfRWYDFFOYQF4KV9xLSEt\r\n" +
            "ycY0kjsdxGKDudWcsATllFzXDCQF6DTNIWECQEA52ePwTjKrVnLTfCLEG4OgHKvl\r\n" +
            "Zud4amthwDyJWoMEH2ChNB2je1N4JLrABOE+hk+OuoKnKAKEjWd8f3Jg/rkCQHj8\r\n" +
            "mQmogHqYWikgP/FSZl518jV48Tao3iXbqvU9Mo2T6yzYNCCqIoDLFWseNVnCTZ0Q\r\n" +
            "b+IfiEf1UeZVV5o4J+ECQDatNnS3V9qYUKjj/krNRD/U0+7eh8S2ylLqD3RlSn9K\r\n" +
            "tYGRMgAtUXtiOEizBH6bd/orzI9V9sw8yBz+ZqIH25Q=\r\n" +
            "-----END RSA PRIVATE KEY-----\r\n";
    private static final FakeMailContext FAKE_MAIL_CONTEXT = FakeMailContext.defaultContext();

    @Test
    public void testDKIMSign() throws MessagingException, IOException,
            FailException {
        String message = "Received: by 10.XX.XX.12 with SMTP id dfgskldjfhgkljsdfhgkljdhfg;\r\n\tTue, 06 Oct 2009 07:37:34 -0700 (PDT)\r\nReturn-Path: <bounce@example.com>\r\nReceived: from example.co.uk (example.co.uk [XX.XXX.125.19])\r\n\tby mx.example.com with ESMTP id dgdfgsdfgsd.97.2009.10.06.07.37.32;\r\n\tTue, 06 Oct 2009 07:37:32 -0700 (PDT)\r\nFrom: apache@bago.org\r\nTo: apache@bago.org\r\n\r\nbody\r\nprova\r\n";

        Mailet mailet = new DKIMSign();

        FakeMailetConfig mci = new FakeMailetConfig("Test",
            FAKE_MAIL_CONTEXT);
        mci
                .setProperty(
                        "signatureTemplate",
                        "v=1; s=selector; d=example.com; h=from:to:received:received; a=rsa-sha256; bh=; b=;");
        mci.setProperty("privateKey", TESTING_PEM);

        mailet.init(mci);

        Mail mail = FakeMail.builder()
            .mimeMessage(new MimeMessage(Session
                .getDefaultInstance(new Properties()),
                new ByteArrayInputStream(message.getBytes())))
            .build();

        mailet.service(mail);

        Mailet m7bit = new ConvertTo7Bit();
        m7bit.init(mci);
        m7bit.service(mail);

        ByteArrayOutputStream rawMessage = new ByteArrayOutputStream();
        mail.getMessage().writeTo(rawMessage);

        MockPublicKeyRecordRetriever mockPublicKeyRecordRetriever = new MockPublicKeyRecordRetriever(
                "v=DKIM1; k=rsa; p=MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDYDaYKXzwVYwqWbLhmuJ66aTAN8wmDR+rfHE8HfnkSOax0oIoTM5zquZrTLo30870YMfYzxwfB6j/Nz3QdwrUD/t0YMYJiUKyWJnCKfZXHJBJ+yfRHr7oW+UW3cVo9CG2bBfIxsInwYe175g9UjyntJpWueqdEIo1c2bhv9Mp66QIDAQAB;",
                "selector", "example.com");
        verify(rawMessage, mockPublicKeyRecordRetriever);
    }

    private List<SignatureRecord> verify(ByteArrayOutputStream rawMessage,
                                         MockPublicKeyRecordRetriever mockPublicKeyRecordRetriever)
            throws MessagingException, FailException {
        List<SignatureRecord> signs = DKIMVerify.verify(new DKIMVerifier(mockPublicKeyRecordRetriever), new MimeMessage(Session.getDefaultInstance(new Properties()), new ByteArrayInputStream(rawMessage.toByteArray())), true);
        assertNotNull(signs);
        assertEquals(1, signs.size());
        return signs;
    }

    @Test
    public void testDKIMSignFuture() throws MessagingException, IOException,
            FailException {
        String message = "Received: by 10.XX.XX.12 with SMTP id dfgskldjfhgkljsdfhgkljdhfg;\r\n\tTue, 06 Oct 2009 07:37:34 -0700 (PDT)\r\nReturn-Path: <bounce@example.com>\r\nReceived: from example.co.uk (example.co.uk [XX.XXX.125.19])\r\n\tby mx.example.com with ESMTP id dgdfgsdfgsd.97.2009.10.06.07.37.32;\r\n\tTue, 06 Oct 2009 07:37:32 -0700 (PDT)\r\nFrom: apache@bago.org\r\nTo: apache@bago.org\r\n\r\nbody\r\nprova\r\n";

        Mailet mailet = new DKIMSign();

        FakeMailetConfig mci = new FakeMailetConfig("Test", FAKE_MAIL_CONTEXT);
        mci.setProperty("signatureTemplate",
                "v=1; t=" + ((System.currentTimeMillis() / 1000) + 1000) + "; s=selector; d=example.com; h=from:to:received:received; a=rsa-sha256; bh=; b=;");
        mci.setProperty("privateKey", TESTING_PEM);

        mailet.init(mci);

        Mail mail = FakeMail.builder()
            .mimeMessage(new MimeMessage(Session
                .getDefaultInstance(new Properties()),
                new ByteArrayInputStream(message.getBytes())))
            .build();

        mailet.service(mail);

        Mailet m7bit = new ConvertTo7Bit();
        m7bit.init(mci);
        m7bit.service(mail);

        ByteArrayOutputStream rawMessage = new ByteArrayOutputStream();
        mail.getMessage().writeTo(rawMessage);

        MockPublicKeyRecordRetriever mockPublicKeyRecordRetriever = new MockPublicKeyRecordRetriever(
                "v=DKIM1; k=rsa; p=MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDYDaYKXzwVYwqWbLhmuJ66aTAN8wmDR+rfHE8HfnkSOax0oIoTM5zquZrTLo30870YMfYzxwfB6j/Nz3QdwrUD/t0YMYJiUKyWJnCKfZXHJBJ+yfRHr7oW+UW3cVo9CG2bBfIxsInwYe175g9UjyntJpWueqdEIo1c2bhv9Mp66QIDAQAB;",
                "selector", "example.com");
        try {
            verify(rawMessage, mockPublicKeyRecordRetriever);
            Assert.fail("Expecting signature to be ignored");
        } catch (PermFailException e) {
            // signature ignored, so fail for missing signatures.
        }
    }


    @Test
    public void testDKIMSignTime() throws MessagingException, IOException,
            FailException {
        String message = "Received: by 10.XX.XX.12 with SMTP id dfgskldjfhgkljsdfhgkljdhfg;\r\n\tTue, 06 Oct 2009 07:37:34 -0700 (PDT)\r\nReturn-Path: <bounce@example.com>\r\nReceived: from example.co.uk (example.co.uk [XX.XXX.125.19])\r\n\tby mx.example.com with ESMTP id dgdfgsdfgsd.97.2009.10.06.07.37.32;\r\n\tTue, 06 Oct 2009 07:37:32 -0700 (PDT)\r\nFrom: apache@bago.org\r\nTo: apache@bago.org\r\n\r\nbody\r\nprova\r\n";

        Mailet mailet = new DKIMSign();

        FakeMailetConfig mci = new FakeMailetConfig("Test", FAKE_MAIL_CONTEXT);
        mci
                .setProperty(
                        "signatureTemplate",
                        "v=1; t=; s=selector; d=example.com; h=from:to:received:received; a=rsa-sha256; bh=; b=;");
        mci.setProperty("privateKey", TESTING_PEM);

        mailet.init(mci);

        Mail mail = FakeMail.builder()
            .mimeMessage(new MimeMessage(Session
                .getDefaultInstance(new Properties()),
                new ByteArrayInputStream(message.getBytes())))
            .build();

        mailet.service(mail);

        Mailet m7bit = new ConvertTo7Bit();
        m7bit.init(mci);
        m7bit.service(mail);

        ByteArrayOutputStream rawMessage = new ByteArrayOutputStream();
        mail.getMessage().writeTo(rawMessage);

        MockPublicKeyRecordRetriever mockPublicKeyRecordRetriever = new MockPublicKeyRecordRetriever(
                "v=DKIM1; k=rsa; p=MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDYDaYKXzwVYwqWbLhmuJ66aTAN8wmDR+rfHE8HfnkSOax0oIoTM5zquZrTLo30870YMfYzxwfB6j/Nz3QdwrUD/t0YMYJiUKyWJnCKfZXHJBJ+yfRHr7oW+UW3cVo9CG2bBfIxsInwYe175g9UjyntJpWueqdEIo1c2bhv9Mp66QIDAQAB;",
                "selector", "example.com");
        verify(rawMessage, mockPublicKeyRecordRetriever);

        List<SignatureRecord> rs = verify(rawMessage, mockPublicKeyRecordRetriever);

        // check we have a valued signatureTimestamp
        Assert.assertNotNull(rs.get(0).getSignatureTimestamp());
        long ref = System.currentTimeMillis() / 1000;
        // Chech that the signature timestamp is in the past 60 seconds.
        Assert.assertTrue(rs.get(0).getSignatureTimestamp() <= ref);
        Assert.assertTrue(rs.get(0).getSignatureTimestamp() >= ref - 60);
    }

    @Test
    public void testDKIMSignMessageAsText() throws MessagingException,
            IOException, FailException {
        MimeMessage mm = new MimeMessage(Session
                .getDefaultInstance(new Properties()));
        mm.addFrom(new Address[]{new InternetAddress("io@bago.org")});
        mm.addRecipient(RecipientType.TO, new InternetAddress("io@bago.org"));
        mm.setText("An 8bit encoded body with \u20ACuro symbol.", "ISO-8859-15");

        Mailet mailet = new DKIMSign();

        FakeMailetConfig mci = new FakeMailetConfig("Test", FAKE_MAIL_CONTEXT);
        mci
                .setProperty(
                        "signatureTemplate",
                        "v=1; s=selector; d=example.com; h=from:to:received:received; a=rsa-sha256; bh=; b=;");
        mci
                .setProperty(
                        "privateKey",
                        TESTING_PEM);

        mailet.init(mci);

        Mail mail = FakeMail.builder()
            .mimeMessage(mm)
            .build();

        Mailet m7bit = new ConvertTo7Bit();
        m7bit.init(mci);

        mailet.service(mail);

        m7bit.service(mail);

        ByteArrayOutputStream rawMessage = new ByteArrayOutputStream();
        mail.getMessage().writeTo(rawMessage);

        MockPublicKeyRecordRetriever mockPublicKeyRecordRetriever = new MockPublicKeyRecordRetriever(
                "v=DKIM1; k=rsa; p=MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDYDaYKXzwVYwqWbLhmuJ66aTAN8wmDR+rfHE8HfnkSOax0oIoTM5zquZrTLo30870YMfYzxwfB6j/Nz3QdwrUD/t0YMYJiUKyWJnCKfZXHJBJ+yfRHr7oW+UW3cVo9CG2bBfIxsInwYe175g9UjyntJpWueqdEIo1c2bhv9Mp66QIDAQAB;",
                "selector", "example.com");

        verify(rawMessage, mockPublicKeyRecordRetriever);
    }

    @Test
    public void testDKIMSignMessageAsObjectConvertedTo7Bit()
            throws MessagingException, IOException, FailException {
        MimeMessage mm = new MimeMessage(Session
                .getDefaultInstance(new Properties()));
        mm.addFrom(new Address[]{new InternetAddress("io@bago.org")});
        mm.addRecipient(RecipientType.TO, new InternetAddress("io@bago.org"));
        mm.setContent("An 8bit encoded body with \u20ACuro symbol.",
                "text/plain; charset=iso-8859-15");
        mm.setHeader("Content-Transfer-Encoding", "8bit");
        mm.saveChanges();

        FakeMailContext FakeMailContext = FAKE_MAIL_CONTEXT;
        FakeMailContext.getServerInfo();
        FakeMailetConfig mci = new FakeMailetConfig("Test", FakeMailContext);
        mci.setProperty(
                "signatureTemplate",
                "v=1; s=selector; d=example.com; h=from:to:received:received; a=rsa-sha256; bh=; b=;");
        mci.setProperty(
                "privateKey",
                TESTING_PEM);

        Mail mail = FakeMail.builder()
            .mimeMessage(mm)
            .build();

        Mailet mailet = new DKIMSign();
        mailet.init(mci);

        Mailet m7bit = new ConvertTo7Bit();
        m7bit.init(mci);
        m7bit.service(mail);

        mailet.service(mail);

        m7bit.service(mail);

        ByteArrayOutputStream rawMessage = new ByteArrayOutputStream();
        mail.getMessage().writeTo(rawMessage);

        MockPublicKeyRecordRetriever mockPublicKeyRecordRetriever = new MockPublicKeyRecordRetriever(
                "v=DKIM1; k=rsa; p=MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDYDaYKXzwVYwqWbLhmuJ66aTAN8wmDR+rfHE8HfnkSOax0oIoTM5zquZrTLo30870YMfYzxwfB6j/Nz3QdwrUD/t0YMYJiUKyWJnCKfZXHJBJ+yfRHr7oW+UW3cVo9CG2bBfIxsInwYe175g9UjyntJpWueqdEIo1c2bhv9Mp66QIDAQAB;",
                "selector", "example.com");
        verify(rawMessage, mockPublicKeyRecordRetriever);
    }

    @Test
    public void testDKIMSignMessageAsObjectNotConverted()
            throws MessagingException, IOException, FailException {
        MimeMessage mm = new MimeMessage(Session
                .getDefaultInstance(new Properties()));
        mm.addFrom(new Address[]{new InternetAddress("io@bago.org")});
        mm.addRecipient(RecipientType.TO, new InternetAddress("io@bago.org"));
        mm.setContent("An 8bit encoded body with \u20ACuro symbol.",
                "text/plain; charset=iso-8859-15");
        mm.setHeader("Content-Transfer-Encoding", "8bit");
        mm.saveChanges();

        FakeMailContext FakeMailContext = FAKE_MAIL_CONTEXT;
        FakeMailContext.getServerInfo();
        FakeMailetConfig mci = new FakeMailetConfig("Test", FakeMailContext);
        mci.setProperty(
                "signatureTemplate",
                "v=1; s=selector; d=example.com; h=from:to:received:received; a=rsa-sha256; bh=; b=;");
        mci.setProperty(
                "privateKey",
                TESTING_PEM);

        Mail mail = FakeMail.builder()
            .mimeMessage(mm)
            .build();

        Mailet mailet = new DKIMSign();
        mailet.init(mci);

        Mailet m7bit = new ConvertTo7Bit();
        m7bit.init(mci);
        // m7bit.service(mail);

        mailet.service(mail);

        m7bit.service(mail);

        ByteArrayOutputStream rawMessage = new ByteArrayOutputStream();
        mail.getMessage().writeTo(rawMessage);

        MockPublicKeyRecordRetriever mockPublicKeyRecordRetriever = new MockPublicKeyRecordRetriever(
                "v=DKIM1; k=rsa; p=MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDYDaYKXzwVYwqWbLhmuJ66aTAN8wmDR+rfHE8HfnkSOax0oIoTM5zquZrTLo30870YMfYzxwfB6j/Nz3QdwrUD/t0YMYJiUKyWJnCKfZXHJBJ+yfRHr7oW+UW3cVo9CG2bBfIxsInwYe175g9UjyntJpWueqdEIo1c2bhv9Mp66QIDAQAB;",
                "selector", "example.com");
        try {
            verify(rawMessage, mockPublicKeyRecordRetriever);
            Assert.fail("Expected PermFail");
        } catch (PermFailException e) {

        }
    }

}
