/*
 * Copyright 2016 Nick Russler
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package mimeparser;

import org.junit.BeforeClass;
import org.junit.Test;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class MimeMessageConverterTest {
    private static MimeMessage messagePlain;
    private static MimeMessage messageMalformedSubject;

    @BeforeClass
    public static void setup() throws FileNotFoundException, MessagingException, URISyntaxException {
        ClassLoader cl = MimeMessageParserTest.class.getClassLoader();

        messagePlain = new MimeMessage(
                null, new FileInputStream(new File(cl.getResource("eml/testPlain.eml").toURI())));
        messageMalformedSubject = new MimeMessage(
                null, new FileInputStream(new File(cl.getResource("eml/testMalformedSubject.eml").toURI())));
    }

    @Test
    public void heuristicallyDecodeInvalidHeaderEncoding_alreadyCorrectlyDecodedSubject() throws UnsupportedEncodingException {
        final String expected = "FW: RE: yeni projeler";
        final String actual = MimeMessageConverter.parseSubject(messagePlain);
        assertThat(actual, equalTo(expected));
    }

    @Test
    public void heuristicallyDecodeInvalidHeaderEncoding_heuristicWorksWithSpaces() throws UnsupportedEncodingException {
        final String expected = "Bananen schält man mit einem Messer";
        assertThat("Bananen schält man mit einem Messer", equalTo(expected));

        final String actual = MimeMessageConverter.parseSubject(messageMalformedSubject);
        assertThat(actual, equalTo(expected));
    }
}
