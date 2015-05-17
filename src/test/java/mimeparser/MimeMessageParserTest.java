/*
 * EML to PDF Converter
 * Copyright (C) 2015 Nick Russler
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package mimeparser;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

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