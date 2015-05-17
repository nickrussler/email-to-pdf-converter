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

import static org.hamcrest.Matchers.equalToIgnoringCase;
import static org.junit.Assert.assertThat;

import javax.mail.MessagingException;
import javax.mail.internet.ContentType;

import org.junit.Test;

/**
 * TestClass.
 * @author Nick Russler
 */
public class ContentTypeCleanerTest {
	@Test
	public void cleanContentType_empty() throws MessagingException {
		ContentType contentType = new ContentType(ContentTypeCleaner.cleanContentType(null, ""));

		assertThat(ContentTypeCleaner.DEFAULT_BASETYPE, equalToIgnoringCase(contentType.getBaseType()));
		assertThat(ContentTypeCleaner.DEFAULT_CHARSET, equalToIgnoringCase(contentType.getParameter("charset")));
	}

	@Test
	public void cleanContentType_semicolonSequenceInParameterList() throws MessagingException {
		ContentType contentType = new ContentType(ContentTypeCleaner.cleanContentType(null, "text/html; ;;;; ;;;   charset=\"utf-16\"  ;;;;"));

		assertThat("text/html", equalToIgnoringCase(contentType.getBaseType()));
		assertThat("utf-16", equalToIgnoringCase(contentType.getParameter("charset")));
	}

	@Test
	public void cleanContentType_colonInsteadOfEqualSign() throws MessagingException {
		ContentType contentType = new ContentType(ContentTypeCleaner.cleanContentType(null, "text/html; charset:\"utf-16\""));

		assertThat("text/html", equalToIgnoringCase(contentType.getBaseType()));
		assertThat("utf-16", equalToIgnoringCase(contentType.getParameter("charset")));
	}

	@Test
	public void cleanContentType_semicolonSequenceInParameterListAndColonInsteadOfEqualSign() throws MessagingException {
		ContentType contentType = new ContentType(ContentTypeCleaner.cleanContentType(null, "text/html; ;;;; charset:\"utf-16\" ;;;;"));

		assertThat("text/html", equalToIgnoringCase(contentType.getBaseType()));
		assertThat("utf-16", equalToIgnoringCase(contentType.getParameter("charset")));
	}

	@Test
	public void cleanContentType_typeAndCharsetMissing() throws MessagingException {
		ContentType contentType = new ContentType(ContentTypeCleaner.cleanContentType(null, "%%% text/html;"));

		assertThat("text/html", equalToIgnoringCase(contentType.getBaseType()));
		assertThat(ContentTypeCleaner.DEFAULT_CHARSET, equalToIgnoringCase(contentType.getParameter("charset")));
	}

	@Test
	public void cleanContentType_typeAndCharsetSomewhereHtml() throws MessagingException {
		ContentType contentType = new ContentType(ContentTypeCleaner.cleanContentType(null, "text/html; utf-16"));

		assertThat("text/html", equalToIgnoringCase(contentType.getBaseType()));
		assertThat("utf-16", equalToIgnoringCase(contentType.getParameter("charset")));
	}

	@Test
	public void cleanContentType_typeAndCharsetSomewherePlainAndCharsetAlias() throws MessagingException {
		ContentType contentType = new ContentType(ContentTypeCleaner.cleanContentType(null, "text/plain; latin1"));

		assertThat("text/plain", equalToIgnoringCase(contentType.getBaseType()));
		assertThat("ISO-8859-1", equalToIgnoringCase(contentType.getParameter("charset")));
	}

	@Test
	public void cleanContentType_quotedPrintable() throws MessagingException {
		ContentType contentType = new ContentType(ContentTypeCleaner.cleanContentType(null, "text/html; charset=3Dutf-16"));

		assertThat("text/html", equalToIgnoringCase(contentType.getBaseType()));
		assertThat("utf-16", equalToIgnoringCase(contentType.getParameter("charset")));
	}

	@Test
	public void cleanContentType_noCharset() throws MessagingException {
		ContentType contentType = new ContentType(ContentTypeCleaner.cleanContentType(null, "text/html;"));

		assertThat("text/html", equalToIgnoringCase(contentType.getBaseType()));
		assertThat(ContentTypeCleaner.DEFAULT_CHARSET, equalToIgnoringCase(contentType.getParameter("charset")));
	}

	@Test
	public void cleanContentType_unknownCharset() throws MessagingException {
		ContentType contentType = new ContentType(ContentTypeCleaner.cleanContentType(null, "text/html; charset=ABCDEF"));

		assertThat("text/html", equalToIgnoringCase(contentType.getBaseType()));
		assertThat(ContentTypeCleaner.DEFAULT_CHARSET, equalToIgnoringCase(contentType.getParameter("charset")));
	}

	@Test
	public void cleanContentType_brokenContentType() throws MessagingException {
		ContentType contentType = new ContentType(ContentTypeCleaner.cleanContentType(null, "BROKEN_STRING"));

		assertThat(ContentTypeCleaner.DEFAULT_BASETYPE, equalToIgnoringCase(contentType.getBaseType()));
		assertThat(ContentTypeCleaner.DEFAULT_CHARSET, equalToIgnoringCase(contentType.getParameter("charset")));
	}
}