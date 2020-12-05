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

import static org.hamcrest.Matchers.equalToIgnoringCase;
import static org.hamcrest.MatcherAssert.assertThat;

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
