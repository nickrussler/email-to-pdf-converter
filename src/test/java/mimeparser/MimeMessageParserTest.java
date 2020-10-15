/*
 * Copyright 2016 Nick Russler
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package mimeparser;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.net.URISyntaxException;
import java.util.HashMap;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.junit.BeforeClass;
import org.junit.Test;

/**
 * TestClass.
 * @author Nick Russler
 */
public class MimeMessageParserTest {
    private static MimeMessage messagePlain;
    private static MimeMessage messageComplex;


    @BeforeClass
    public static void setup() throws FileNotFoundException, MessagingException, URISyntaxException {
        ClassLoader cl = MimeMessageParserTest.class.getClassLoader();

        messagePlain = new MimeMessage(null, new FileInputStream(new File(cl.getResource("eml/testPlain.eml").toURI())));
        messageComplex = new MimeMessage(null, new FileInputStream(new File(cl.getResource("eml/testHtml.eml").toURI())));
    }

    @Test
    public void printStructure_foundAllPartsPlain() throws Exception {
        assertThat(MimeMessageParser.printStructure(messagePlain), containsString("text/plain"));
    }

    @Test
    public void printStructure_foundAllPartsComplex() throws Exception {
        String structure = MimeMessageParser.printStructure(messageComplex);

        assertThat(structure, containsString("multipart/mixed"));
        assertThat(structure, containsString("multipart/related"));
        assertThat(structure, containsString("multipart/alternative"));

        assertThat(structure, containsString("text/plain"));
        assertThat(structure, containsString("text/html"));

        assertThat(structure, containsString("image/gif"));
    }

    @Test
    public void findBodyPart_foundMainBodyInPlain() throws Exception {
        assertThat("Hallo, Guten Tag!", equalTo(MimeMessageParser.findBodyPart(messagePlain).getEntry()));
    }

    @Test
    public void findBodyPart_foundMainBodyInComplex() throws Exception {
        assertThat("text/html", equalTo(MimeMessageParser.findBodyPart(messageComplex).getContentType().getBaseType()));
    }

    @Test
    public void getInlineImageMap_foundImagesInComplex() throws Exception {
        HashMap<String, MimeObjectEntry<String>> inlineImageMap = MimeMessageParser.getInlineImageMap(messageComplex);

        assertThat(inlineImageMap.get("<ae0357e57f04b8347f7621662cb63855.gif>"), is(not(nullValue())));
        assertThat(inlineImageMap.get("<4c837ed463ad29c820668e835a270e8a.gif>"), is(not(nullValue())));
    }
}
