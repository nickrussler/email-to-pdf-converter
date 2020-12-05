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

import com.google.common.base.Strings;
import com.google.common.io.ByteStreams;
import util.Logger;

import javax.mail.MessagingException;
import javax.mail.internet.ContentType;
import javax.mail.internet.MimePart;
import javax.mail.internet.MimeUtility;
import javax.mail.internet.ParseException;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

/**
 * Sanitizes contentType strings.
 * @author Nick Russler
 */
public class ContentTypeCleaner {
    public static final String DEFAULT_BASETYPE = "text/plain";
    public static final String DEFAULT_CHARSET = "utf-8";
    private static final String DEFAULT_CONTENTTYPE = DEFAULT_BASETYPE + "; charset=\"" + DEFAULT_CHARSET + "\"";
    private static final Pattern SEMICOLON_SEQUENCE_IN_PARAMS_REGEX = Pattern.compile(";[\\s;]*;");
    private static final Pattern COLON_AS_PARAM_DELIM_REGEX = Pattern.compile("([^=:]*)(=|:)(.*?(;|\\z))");

    /**
     * Try to parse the given contentType String into a ContentType instance.
     * @param contentType
     * @return new ContentType instance, or null if not parsable
     */
    private static ContentType parseContentType(String contentType) {
        try {
            return new ContentType(contentType);
        } catch (ParseException e) {
            return null;
        }
    }

    /**
     * Try to parse the charset parameter of the ContentType into a Charset instance.
     * @param contentType
     * @return new Charset instance, or null if not parsable
     */
    private static Charset parseCharset(ContentType contentType) {
        try {
            return Charset.forName(contentType.getParameter("charset"));
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * fixes invalid semicolon sequences in the parameter list e.g.: "text/html; ;;;;; ;;;; ;...".
     * @param contentType ContentType
     * @return cleaned contentType
     */
    private static String fixContentType_semicolonSequenceInParams(String contentType) {
        return SEMICOLON_SEQUENCE_IN_PARAMS_REGEX.matcher(contentType).replaceAll(";");
    }

    /**
     * cleans misusage of : instead of = in parameter list e.g.: "text/html; charset:utf-8".
     * @param contentType ContentType
     * @return cleaned contentType
     */
    private static String fixContentType_colonAsParamDelim(String contentType) {
        return COLON_AS_PARAM_DELIM_REGEX.matcher(contentType).replaceAll("$1=$3");
    }

    /**
     * Try to find the base type and charset somewhere in the string.
     * @param contentType ContentType
     * @return contentType string consisting of the found base type and charset
     */
    private static String fixContentType_findByBruteForce(String contentType) {
        contentType = contentType.toLowerCase();

        String baseType = null;
        if (contentType.contains("text/html")) {
            baseType = "text/html";
        } else if (contentType.contains("text/plain")) {
            baseType = "text/plain";
        }

        // iterate all available charsets and look for them in the contentType string
        String charset = null;
        for (Charset c : Charset.availableCharsets().values()) {
            if (contentType.contains(c.name().toLowerCase())) {
                charset = c.name();
                break;
            }
        }

        // could not find the charset, check for aliases
        if (charset == null) {
            for (Charset c : Charset.availableCharsets().values()) {
                for (String alias : c.aliases()) {
                    if (contentType.contains(alias.toLowerCase())) {
                        charset = c.name();
                        break;
                    }
                }
            }
        }

        // we found a basetype and a charset
        if (baseType != null && charset != null) {
            return String.format("%s; charset=\"%s\"", baseType, charset);
        }

        // only found a basetype, use default charset
        if (baseType != null) {
            return String.format("%s; charset=\"%s\"", baseType, DEFAULT_CHARSET);
        }

        return null;
    }

    /**
     * Try to decode the contentType String as quoted-printable String into a ContentType.
     * @param contentType
     * @return new ContentType instance or null
     */
    private static ContentType decodeContentTypeAsQuotedPrintable(String contentType) {
        try {
            ByteArrayInputStream baos = new ByteArrayInputStream(contentType.getBytes(StandardCharsets.UTF_8));
            InputStream decode = MimeUtility.decode(baos, "quoted-printable");
            String contentTypeString = new String(ByteStreams.toByteArray(decode), StandardCharsets.UTF_8);
            return new ContentType(contentTypeString);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Check if the contentType can be parsed. If not return a fixed version.
     * When thats not possible return a default contentType string.
     * @param contentType
     * @return Fixed contentType string or default
     * @throws ParseException
     */
    private static ContentType getParsableContentType(String contentType) throws ParseException {
        Logger.debug("Encountered an unparsable contenttype, try to fix it.");

        // we can't fix an empty contentType, fallback to default
        if (Strings.isNullOrEmpty(contentType)) {
            Logger.debug("ContentType empty, fallback to \"%s\"", DEFAULT_CONTENTTYPE);
            return new ContentType(DEFAULT_CONTENTTYPE);
        }

        ContentType tmp = parseContentType(fixContentType_semicolonSequenceInParams(contentType));
        if (tmp != null) {
            Logger.debug("Fix succeeded (1)");
            return tmp;
        }

        tmp = parseContentType(fixContentType_colonAsParamDelim(contentType));
        if (tmp != null) {
            Logger.debug("Fix succeeded (2)");
            return tmp;
        }

        // Neither did work, lets try to use clean1 and clean2 in conjunction
        tmp = parseContentType(fixContentType_semicolonSequenceInParams(fixContentType_colonAsParamDelim(contentType)));
        if (tmp != null) {
            Logger.debug("Fix succeeded (1&2)");
            return tmp;
        }

        // this is a rather desperate approach but lets try it nevertheless
        tmp = parseContentType(fixContentType_findByBruteForce(contentType));
        if (tmp != null) {
            Logger.debug("Fix succeeded (3)");
            return tmp;
        }

        Logger.debug("Encountered broken ContentType, fallback to default: %s", DEFAULT_CONTENTTYPE);
        return new ContentType(DEFAULT_CONTENTTYPE);
    }

    /**
     * Attempt to repair the given contentType if broken.
     *
     * @param mp MimePart
     * @param contentType ContentType
     * @return fixed contentType String
     * @throws MessagingException
     */
    public static String cleanContentType(MimePart mp, String contentType) throws MessagingException {
        ContentType ct = parseContentType(contentType);

        if (ct == null) {
            ct = getParsableContentType(contentType);
        }

        if (ct.getBaseType().equalsIgnoreCase("text/plain") || ct.getBaseType().equalsIgnoreCase("text/html")) {
            Charset charset = parseCharset(ct);
            if (charset == null) {
                Logger.debug("Charset of the ContentType could not be read, try to decode the contentType as quoted-printable");

                ContentType ctTmp = decodeContentTypeAsQuotedPrintable(contentType);
                if (parseCharset(ctTmp) != null) {
                    ct = ctTmp;
                } else {
                    ct.setParameter("charset", ContentTypeCleaner.DEFAULT_CHARSET);
                }
            }
        }

        return ct.toString();
    }
}
