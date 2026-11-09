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

package render;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItems;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import cli.Main;
import com.google.common.io.Files;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import mimeparser.MimeMessageConverter;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import util.LogLevel;
import util.Logger;

@RunWith(Parameterized.class)
public class RendererE2ETest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private final RenderOptions.Renderer renderer;

    public RendererE2ETest(RenderOptions.Renderer renderer) {
        this.renderer = renderer;
    }

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> renderers() {
        return Arrays.asList(new Object[][]{{RenderOptions.Renderer.WKHTMLTOPDF}, {RenderOptions.Renderer.CHROME}});
    }

    @BeforeClass
    public static void requireWkhtmltopdf() throws Exception {
        Process process = new ProcessBuilder("wkhtmltopdf", "--version").redirectErrorStream(true).start();
        assertTrue("wkhtmltopdf must be installed and available on PATH", process.waitFor() == 0);
    }

    @Test
    public void rendersHtmlEmail() throws Exception {
        File workDir = temporaryFolder.newFolder();
        File eml = resource("eml/testHtml.eml");
        File output = new File(workDir, "html.pdf");

        convert(eml, output, "A4", false, false, false);

        try (PDDocument document = Loader.loadPDF(output)) {
            assertPageSize(document, PaperSize.fromName("A4"));
            String text = normalizedText(document);
            assertThat(text, containsString("Testing Manuel Lemos' MIME E-mail composing and sending PHP class: HTML message"));
            assertThat(text, containsString("Hello Manuel,"));
            assertHasEmbeddedImage(document, 150, 35);
        }
    }

    @Test
    public void rendersPlainEmailOnLetter() throws Exception {
        File workDir = temporaryFolder.newFolder();
        File eml = resource("eml/testPlain.eml");
        File output = new File(workDir, "plain.pdf");

        convert(eml, output, "Letter", false, false, false);

        try (PDDocument document = Loader.loadPDF(output)) {
            assertPageSize(document, PaperSize.fromName("Letter"));
            String text = normalizedText(document);
            assertThat(text, containsString("FW: RE: yeni projeler"));
            assertThat(text, containsString("Hallo, Guten Tag!"));
        }
    }

    @Test
    public void rendersNonAsciiText() throws Exception {
        File workDir = temporaryFolder.newFolder();
        File eml = writeHtmlEml(
                workDir, "non-ascii.eml", "Non ASCII", "<html><body><p>Grüße, Привет, Καλημέρα</p></body></html>");
        File output = new File(workDir, "non-ascii.pdf");

        convert(eml, output, "A4", false, false, false);

        try (PDDocument document = Loader.loadPDF(output)) {
            String text = normalizedText(document);
            assertThat(text, containsString("Grüße"));
            assertThat(text, containsString("Привет"));
            assertThat(text, containsString("Καλημέρα"));
        }
    }

    @Test
    public void javascriptIsDisabledByDefault() throws Exception {
        File workDir = temporaryFolder.newFolder();
        String bodyMarker = "BODY_MARKER_JS_DEFAULT";
        String scriptMarker = "JS_MARKER_DEFAULT";
        File eml = writeHtmlEml(
                workDir, "js-default.eml", "JS default", "<html><body><p>" + bodyMarker + "</p><script>document.body.appendChild(document.createTextNode(' " + scriptMarker + " '));</script></body></html>");
        File output = new File(workDir, "js-default.pdf");

        convert(eml, output, "A4", true, false, false);

        try (PDDocument document = Loader.loadPDF(output)) {
            String text = normalizedText(document);
            assertThat(text, containsString(bodyMarker));
            assertTrue("JavaScript must not execute by default", !text.contains(scriptMarker));
        }
    }

    @Test
    public void javascriptCanBeEnabledExplicitly() throws Exception {
        File workDir = temporaryFolder.newFolder();
        String bodyMarker = "BODY_MARKER_JS_OPT_IN";
        String scriptMarker = "JS_MARKER_OPT_IN";
        File eml = writeHtmlEml(
                workDir, "js-opt-in.eml", "JS opt in", "<html><body><p>" + bodyMarker + "</p><script>document.body.appendChild(document.createTextNode(' " + scriptMarker + " '));</script></body></html>");
        File output = new File(workDir, "js-opt-in.pdf");

        runMain(
                "--renderer", rendererName(), "--dangerous-enable-javascript", "-o", output.getAbsolutePath(), "-hh", eml.getAbsolutePath());

        try (PDDocument document = Loader.loadPDF(output)) {
            String text = normalizedText(document);
            assertThat(text, containsString(bodyMarker));
            assertThat(text, containsString(scriptMarker));
        }
    }

    @Test
    public void extractsAttachments() throws Exception {
        File workDir = temporaryFolder.newFolder();
        File eml = resource("eml/testAttachments.eml");
        File output = new File(workDir, "attachments.pdf");
        File attachmentsDir = new File(workDir, "attachments");

        convert(eml, output, "A4", true, true, false, attachmentsDir.getAbsolutePath());

        assertTrue("Expected extracted attachments directory", attachmentsDir.isDirectory());
        List<String> attachments = Arrays.asList(attachmentsDir.list());
        assertThat(attachments, hasItems("IMAG0144.jpg", "IMAG0145.jpg"));
    }

    @Test
    public void rendersLongHeaderWithoutClipping() throws Exception {
        File workDir = temporaryFolder.newFolder();
        StringBuilder subject = new StringBuilder("LONG_HEADER_START ");
        for (int i = 0; i < 30; i++) {
            subject.append("abcdefghij ");
        }
        subject.append("LONG_HEADER_END");
        String bodyMarker = "BODY_MARKER_LONG_HEADER";
        File eml = writeHtmlEml(
                workDir, "long-header.eml", subject.toString(), "<html><body><p>" + bodyMarker + "</p></body></html>");
        File output = new File(workDir, "long-header.pdf");

        convert(eml, output, "A4", false, false, false);

        try (PDDocument document = Loader.loadPDF(output)) {
            String text = normalizedText(document);
            assertThat(text, containsString("LONG_HEADER_START"));
            assertThat(text, containsString("LONG_HEADER_END"));
            assertThat(text, containsString(bodyMarker));
        }
    }

    @Test
    public void rendersLandscapeOrientation() throws Exception {
        File workDir = temporaryFolder.newFolder();
        String bodyMarker = "LANDSCAPE_MARKER";
        File eml = writeHtmlEml(
                workDir, "landscape.eml", "Landscape", "<html><head><style>@page WordSection1 { size:8.5in 11.0in; }</style></head><body><p>" + bodyMarker + "</p></body></html>");
        File output = new File(workDir, "landscape.pdf");

        runMain(
                "--renderer", rendererName(), "--orientation", "landscape", "-o", output.getAbsolutePath(), "-hh", eml.getAbsolutePath());

        try (PDDocument document = Loader.loadPDF(output)) {
            assertPageSize(document, PaperSize.fromName("A4"), RenderOptions.Orientation.LANDSCAPE);
            assertThat(normalizedText(document), containsString(bodyMarker));
        }
    }

    @Test
    public void rendersEverySupportedPageSize() throws Exception {
        File workDir = temporaryFolder.newFolder();
        for (PaperSize paperSize : PaperSize.all()) {
            File eml = writeHtmlEml(
                    workDir, "page-" + paperSize.getName() + ".eml", "Page size " + paperSize.getName(), "<html><body><p>PAGE_SIZE_" + paperSize.getName() + "</p></body></html>");
            File output = new File(workDir, "page-" + paperSize.getName() + ".pdf");

            convert(eml, output, paperSize.getName(), true, false, false);

            try (PDDocument document = Loader.loadPDF(output)) {
                assertPageSize(document, paperSize);
            }
        }
    }

    private void convert(File eml, File output, String pageSize, boolean hideHeaders, boolean extractAttachments, boolean javaScriptEnabled) throws Exception {
        convert(eml, output, pageSize, hideHeaders, extractAttachments, javaScriptEnabled, null);
    }

    private void convert(
                         File eml, File output, String pageSize, boolean hideHeaders, boolean extractAttachments, boolean javaScriptEnabled, String attachmentsDir) throws Exception {
        RenderOptions options = new RenderOptions(renderer, null, null, pageSize, "300", javaScriptEnabled, null);

        LogLevel old = Logger.level;
        Logger.level = LogLevel.Error;
        try {
            MimeMessageConverter.convertToPdf(
                    eml.getAbsolutePath(), output.getAbsolutePath(), hideHeaders, false, extractAttachments, attachmentsDir, options);
        } finally {
            Logger.level = old;
        }

        assertTrue("Expected a generated PDF: " + output, output.isFile() && output.length() > 0);
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

    private String rendererName() {
        return renderer == RenderOptions.Renderer.CHROME ? "chrome" : "wkhtmltopdf";
    }

    private static File resource(String name) throws Exception {
        return new File(RendererE2ETest.class.getClassLoader().getResource(name).toURI());
    }

    private static File writeHtmlEml(File workDir, String name, String subject, String body) throws IOException {
        File eml = new File(workDir, name);
        String content = "Mime-Version: 1.0\r\n" + "Date: Thu, 11 Dec 2014 05:20:45 -0800\r\n" + "Message-ID: <" + name + "@example.com>\r\n" + "From: sender@example.com\r\n" + "To: recipient@example.com\r\n" + "Subject: " + subject + "\r\n" + "Content-Type: text/html; charset=utf-8\r\n" + "Content-Transfer-Encoding: 8bit\r\n" + "\r\n" + body;
        Files.asCharSink(eml, StandardCharsets.UTF_8).write(content);
        return eml;
    }

    private static void assertPageSize(PDDocument document, PaperSize paperSize) {
        assertPageSize(document, paperSize, RenderOptions.Orientation.PORTRAIT);
    }

    private static void assertPageSize(PDDocument document, PaperSize paperSize, RenderOptions.Orientation orientation) {
        double expectedWidth = orientation == RenderOptions.Orientation.LANDSCAPE ? paperSize.getHeightPoints() : paperSize.getWidthPoints();
        double expectedHeight = orientation == RenderOptions.Orientation.LANDSCAPE ? paperSize.getWidthPoints() : paperSize.getHeightPoints();
        assertEquals(expectedWidth, document.getPage(0).getMediaBox().getWidth(), 1.0f);
        assertEquals(expectedHeight, document.getPage(0).getMediaBox().getHeight(), 1.0f);
    }

    private static String normalizedText(PDDocument document) throws IOException {
        return new PDFTextStripper().getText(document).replaceAll("\\s+", " ");
    }

    private static void assertHasEmbeddedImage(PDDocument document, int expectedWidth, int expectedHeight) throws IOException {
        PDResources resources = document.getPage(0).getResources();
        for (COSName name : resources.getXObjectNames()) {
            PDXObject xObject = resources.getXObject(name);
            if (xObject instanceof PDImageXObject) {
                PDImageXObject image = (PDImageXObject) xObject;
                if (image.getWidth() == expectedWidth && image.getHeight() == expectedHeight) {
                    return;
                }
            }
        }
        throw new AssertionError("Expected an embedded image sized " + expectedWidth + "x" + expectedHeight);
    }
}
