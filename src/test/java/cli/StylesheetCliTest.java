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

package cli;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.common.io.Files;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.SystemErrRule;
import org.junit.rules.TemporaryFolder;
import util.LogLevel;
import util.Logger;

public class StylesheetCliTest {
    @Rule
    public final SystemErrRule systemErrRule = new SystemErrRule().enableLog();

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void missingStylesheetFailsWithoutCreatingOutput() throws Exception {
        File email = writeEmail("missing.css.eml", "<p>MISSING_STYLESHEET_MARKER</p>");
        File output = new File(temporaryFolder.newFolder(), "missing.pdf");

        runMain(
                "--renderer", "chrome", "--stylesheet", new File(temporaryFolder.getRoot(), "does-not-exist.css").getAbsolutePath(), "-o", output.getAbsolutePath(), "-hh", email.getAbsolutePath());

        assertFalse(output.exists());
        assertThat(systemErrRule.getLog(), containsString("Could not read stylesheet"));
    }

    @Test
    public void wkhtmltopdfRejectsStylesheet() throws Exception {
        File email = writeEmail("unsupported.eml", "<p>UNSUPPORTED_STYLESHEET_MARKER</p>");
        File stylesheet = writeStylesheet("@page { size: auto !important; }");
        File output = new File(temporaryFolder.newFolder(), "unsupported.pdf");

        runMain(
                "--renderer", "wkhtmltopdf", "--stylesheet", stylesheet.getAbsolutePath(), "-o", output.getAbsolutePath(), "-hh", email.getAbsolutePath());

        assertFalse(output.exists());
        assertThat(systemErrRule.getLog(), containsString("--stylesheet is only supported by the Chrome renderer"));
    }

    @Test
    public void cliReadsStylesheetAndOverridesAppliedPortraitPageRule() throws Exception {
        File email = writeEmail(
                "applied-page.eml", "<html><head><style>@page WordSection1 { size: 8.5in 11in; } body { page: WordSection1; }</style></head><body><p>CLI_STYLESHEET_MARKER</p></body></html>");
        File stylesheet = writeStylesheet("@page { size: auto !important; }");
        File output = new File(temporaryFolder.newFolder(), "applied-page.pdf");

        runMain(
                "--renderer", "chrome", "--orientation", "landscape", "--stylesheet", stylesheet.getAbsolutePath(), "-o", output.getAbsolutePath(), "-hh", email.getAbsolutePath());

        assertTrue(output.isFile());
        try (PDDocument document = Loader.loadPDF(output)) {
            assertTrue(document.getPage(0).getMediaBox().getWidth() > document.getPage(0).getMediaBox().getHeight());
        }
    }

    private File writeEmail(String name, String body) throws IOException {
        File email = new File(temporaryFolder.newFolder(), name);
        String content = "Mime-Version: 1.0\r\n" + "Date: Thu, 11 Dec 2014 05:20:45 -0800\r\n" + "Message-ID: <" + name + "@example.com>\r\n" + "From: sender@example.com\r\n" + "To: recipient@example.com\r\n" + "Subject: Stylesheet test\r\n" + "Content-Type: text/html; charset=utf-8\r\n" + "Content-Transfer-Encoding: 8bit\r\n" + "\r\n" + body;
        Files.asCharSink(email, StandardCharsets.UTF_8).write(content);
        return email;
    }

    private File writeStylesheet(String css) throws IOException {
        File stylesheet = new File(temporaryFolder.newFolder(), "print.css");
        Files.asCharSink(stylesheet, StandardCharsets.UTF_8).write(css);
        return stylesheet;
    }

    private static void runMain(String... args) {
        LogLevel old = Logger.level;
        Logger.level = LogLevel.Error;
        try {
            Main.main(args);
        } finally {
            Logger.level = old;
        }
    }
}
