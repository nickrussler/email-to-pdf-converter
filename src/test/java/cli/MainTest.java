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
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.common.io.Files;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import mimeparser.MimeMessageConverter;
import mimeparser.PdfContentHeightMeasurer;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import util.LogLevel;
import util.Logger;

/**
 * TestClass.
 *
 * @author Nick Russler
 */
public class MainTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    public static void convertFolderTest() throws FileNotFoundException, UnsupportedEncodingException {
        PrintWriter writer = new PrintWriter("d:\\mylog.txt", "UTF-8");
        writer.println("Log start time: " + new Date());
        writer.println("-----------------------");
        writer.flush();

        Logger.level = LogLevel.Debug;

        String outPath = System.getProperty("user.home") + "/Desktop/test.pdf";

        File rootDir = new File("D:\\devel\\email_training\\");
        for (File f : Files.fileTraverser().depthFirstPreOrder(rootDir)) {
            try {
                if (f.isDirectory()) {
                    continue;
                }

                System.out.println(f.getAbsolutePath());
                writer.println("Process file: " + f.getAbsolutePath());

                Main.main(new String[]{"-d", "-o", outPath, f.getAbsolutePath()});

                // Desktop.getDesktop().open(new File(outPath));
            } catch (Exception e) {
                System.out.println(f.getAbsolutePath() + " - " + e.getMessage());

                writer.println(f.getAbsolutePath() + " - " + e.getMessage());
                writer.println(Throwables.getStackTraceAsString(e));
                writer.println("##########################");
                writer.flush();
            }
        }

        writer.println("-----------------------");
        writer.println("Log end time: " + new Date());
        writer.flush();

        writer.close();
    }

    @Test
    public void main_simplePlainMessage() throws IOException, URISyntaxException {
        File tmpPdf = new File(temporaryFolder.newFolder(), "output.pdf");
        String eml = new File(MainTest.class.getClassLoader().getResource("eml/testPlain.eml").toURI()).getAbsolutePath();

        String[] args = new String[]{"-o", tmpPdf.getAbsolutePath(), "-s", "Letter", eml};

        LogLevel old = Logger.level;
        Logger.level = LogLevel.Error;

        Main.main(args);

        Logger.level = old;

        assertTrue("Expected a generated PDF: " + tmpPdf, tmpPdf.isFile());
        assertTrue("Generated PDF must not be empty: " + tmpPdf, tmpPdf.length() > 0);

        assertSinglePagePdf(tmpPdf, PDRectangle.LETTER, "FW: RE: yeni projeler", "bolo yükselulupe", "y.aaaat@ibbx.com", "naaa.rddder@aaal.com", "Hallo, Guten Tag!");

        if (!tmpPdf.delete()) {
            tmpPdf.deleteOnExit();
        }
    }

    @Test
    public void main_simplePlainMessageWithCC() throws IOException, URISyntaxException {
        File tmpPdf = new File(temporaryFolder.newFolder(), "output.pdf");
        String eml = new File(MainTest.class.getClassLoader().getResource("eml/testPlainWithCC.eml").toURI()).getAbsolutePath();

        String[] args = new String[]{"-o", tmpPdf.getAbsolutePath(), "-s", "Letter", eml};

        LogLevel old = Logger.level;
        Logger.level = LogLevel.Error;

        Main.main(args);

        Logger.level = old;

        assertTrue("Expected a generated PDF: " + tmpPdf, tmpPdf.isFile());
        assertTrue("Generated PDF must not be empty: " + tmpPdf, tmpPdf.length() > 0);

        assertSinglePagePdf(tmpPdf, PDRectangle.LETTER, "FW: RE: yeni projeler", "bolo yükselulupe", "y.aaaat@ibbx.com", "naaa.rddder@aaal.com", "naaa2.rddder2@aaal.com", "Hallo, Guten Tag!");

        if (!tmpPdf.delete()) {
            tmpPdf.deleteOnExit();
        }
    }

    @Test
    public void main_messageWithAttachmentList() throws IOException, URISyntaxException {
        File tmpPdf = new File(temporaryFolder.newFolder(), "output.pdf");
        String eml = new File(MainTest.class.getClassLoader().getResource("eml/testAttachments.eml").toURI()).getAbsolutePath();

        String[] args = new String[]{"-o", tmpPdf.getAbsolutePath(), "-aa", "-s", "Letter", eml};

        LogLevel old = Logger.level;
        Logger.level = LogLevel.Error;

        Main.main(args);

        Logger.level = old;

        assertTrue("Expected a generated PDF: " + tmpPdf, tmpPdf.isFile());
        assertTrue("Generated PDF must not be empty: " + tmpPdf, tmpPdf.length() > 0);

        assertSinglePagePdf(tmpPdf, PDRectangle.LETTER, "Attachments:", "IMAG0145.jpg", "IMAG0144.jpg");

        if (!tmpPdf.delete()) {
            tmpPdf.deleteOnExit();
        }
    }

    @Test
    public void main_messageWithoutAttachmentList() throws IOException, URISyntaxException {
        File tmpPdf = new File(temporaryFolder.newFolder(), "output.pdf");
        String eml = new File(MainTest.class.getClassLoader().getResource("eml/testAttachments.eml").toURI()).getAbsolutePath();

        runMain("-o", tmpPdf.getAbsolutePath(), eml);

        assertTrue("Expected a generated PDF: " + tmpPdf, tmpPdf.isFile());
        assertTrue("Generated PDF must not be empty: " + tmpPdf, tmpPdf.length() > 0);

        assertSinglePagePdf(tmpPdf, PDRectangle.A4, "and katakana");
        assertPdfTextDoesNotContain(tmpPdf, "Attachments:", "IMAG0144.jpg", "IMAG0145.jpg");

        if (!tmpPdf.delete()) {
            tmpPdf.deleteOnExit();
        }
    }

    @Test
    public void main_htmlMessage() throws IOException, URISyntaxException {
        File tmpPdf = new File(temporaryFolder.newFolder(), "output.pdf");
        String eml = new File(MainTest.class.getClassLoader().getResource("eml/testHtml.eml").toURI()).getAbsolutePath();

        String[] args = new String[]{"-o", tmpPdf.getAbsolutePath(), eml};

        LogLevel old = Logger.level;
        Logger.level = LogLevel.Error;

        Main.main(args);

        Logger.level = old;

        assertTrue("Expected a generated PDF: " + tmpPdf, tmpPdf.isFile());
        assertTrue("Generated PDF must not be empty: " + tmpPdf, tmpPdf.length() > 0);

        assertSinglePagePdf(
                tmpPdf, PDRectangle.A4, "Testing Manuel Lemos' MIME E-mail composing and sending PHP class: HTML message", "mlemos@acm.org", "mlemos@linux.local", "Hello Manuel,", "MIME E-mail message composing and sending PHP class is working as expected.");
        // The fixture references two cid images; the renderer emits the <img> logo as an image XObject.
        assertHasEmbeddedImage(tmpPdf, 150, 35);

        if (!tmpPdf.delete()) {
            tmpPdf.deleteOnExit();
        }
    }

    @Test
    public void main_msg_htmlMessage() throws IOException, URISyntaxException {
        File tmpPdf = new File(temporaryFolder.newFolder(), "output.pdf");
        String msgFilePath = new File(MainTest.class.getClassLoader().getResource("msg/testHtml.msg").toURI()).getAbsolutePath();

        String[] args = new String[]{"-o", tmpPdf.getAbsolutePath(), msgFilePath};

        LogLevel old = Logger.level;
        Logger.level = LogLevel.Error;

        Main.main(args);

        Logger.level = old;

        assertTrue("Expected a generated PDF: " + tmpPdf, tmpPdf.isFile());
        assertTrue("Generated PDF must not be empty: " + tmpPdf, tmpPdf.length() > 0);

        assertSinglePagePdf(tmpPdf, PDRectangle.A4, "SubjectofTestemail", "to@test.com", "cc@test.com", "Main body of Testemail");

        if (!tmpPdf.delete()) {
            tmpPdf.deleteOnExit();
        }
    }

    @Test
    public void main_msg_richtextMessage() throws IOException, URISyntaxException {
        File tmpPdf = new File(temporaryFolder.newFolder(), "output.pdf");
        String msgFilePath = new File(MainTest.class.getClassLoader().getResource("msg/testRichtext.msg").toURI()).getAbsolutePath();

        runMain("-o", tmpPdf.getAbsolutePath(), msgFilePath);

        assertTrue("Expected a generated PDF: " + tmpPdf, tmpPdf.isFile());
        assertTrue("Generated PDF must not be empty: " + tmpPdf, tmpPdf.length() > 0);

        assertSinglePagePdf(
                tmpPdf, PDRectangle.A4, "Hello, this is a test in rich text format", "Regards,", "John Doe", "john.doe@outlook.com", "RichText Test");
        assertPdfTextDoesNotContain(tmpPdf, "Default Paragraph Font", "Normal Table", "HYPERLINK \"mailto:", "E-MailFormatvorlage");

        if (!tmpPdf.delete()) {
            tmpPdf.deleteOnExit();
        }
    }

    @Test
    public void main_simplePlainMessageHiddenHeaders() throws IOException, URISyntaxException {
        File tmpPdf = new File(temporaryFolder.newFolder(), "output.pdf");
        String eml = new File(MainTest.class.getClassLoader().getResource("eml/testPlain.eml").toURI()).getAbsolutePath();

        runMain("-o", tmpPdf.getAbsolutePath(), "-hh", eml);

        assertTrue("Expected a generated PDF: " + tmpPdf, tmpPdf.isFile());
        assertTrue("Generated PDF must not be empty: " + tmpPdf, tmpPdf.length() > 0);

        assertSinglePagePdf(tmpPdf, PDRectangle.A4, "Hallo, Guten Tag!");
        assertPdfTextDoesNotContain(tmpPdf, "FW: RE: yeni projeler", "bolo yükselulupe", "y.aaaat@ibbx.com", "naaa.rddder@aaal.com");

        if (!tmpPdf.delete()) {
            tmpPdf.deleteOnExit();
        }
    }

    @Test
    public void main_localFileReferenceBlockedByDefault() throws IOException {
        File workDir = temporaryFolder.newFolder();
        File marker = new File(workDir, "local-file-marker.html");
        String markerText = "LOCAL_FILE_MARKER_7d2a";
        Files.asCharSink(marker, StandardCharsets.UTF_8).write("<html><body><p>" + markerText + "</p></body></html>");

        String bodyText = "BODY_MARKER_LOCAL_FILE_7d2a";
        File eml = writeHtmlEml(
                workDir, "local-file-reference.eml", "Local file reference", "sender@example.com", "<html><body><p>" + bodyText + "</p><iframe src=\"" + marker.toURI() + "\" style=\"width:600px;height:120px\"></iframe></body></html>");
        File tmpPdf = new File(workDir, "output.pdf");

        runMain("-o", tmpPdf.getAbsolutePath(), "-s", "Letter", eml.getAbsolutePath());

        assertTrue("Expected a generated PDF: " + tmpPdf, tmpPdf.isFile());
        assertTrue("Generated PDF must not be empty: " + tmpPdf, tmpPdf.length() > 0);
        assertSinglePagePdf(tmpPdf, PDRectangle.LETTER, bodyText);
        assertPdfTextDoesNotContain(tmpPdf, markerText);

        if (!tmpPdf.delete()) {
            tmpPdf.deleteOnExit();
        }
    }

    @Test
    public void main_localFileReferenceRequiresExplicitExtParam() throws Exception {
        File workDir = temporaryFolder.newFolder();
        File marker = new File(workDir, "local-file-marker.html");
        String markerText = "LOCAL_FILE_MARKER_8e3b";
        Files.asCharSink(marker, StandardCharsets.UTF_8).write("<html><body><p>" + markerText + "</p></body></html>");

        String bodyText = "BODY_MARKER_LOCAL_FILE_8e3b";
        File eml = writeHtmlEml(
                workDir, "local-file-reference.eml", "Local file reference", "sender@example.com", "<html><body><p>" + bodyText + "</p><iframe src=\"" + marker.toURI() + "\" style=\"width:600px;height:120px\"></iframe></body></html>");
        File tmpPdf = new File(workDir, "output.pdf");

        // This positive control documents the public-API bypass through extParams.
        LogLevel old = Logger.level;
        Logger.level = LogLevel.Error;
        try {
            MimeMessageConverter.convertToPdf(
                    eml.getAbsolutePath(), tmpPdf.getAbsolutePath(), false, false, false, null, Arrays.asList("--page-size", "Letter", "--dpi", "300", "--enable-local-file-access"));
        } finally {
            Logger.level = old;
        }

        assertTrue("Expected a generated PDF: " + tmpPdf, tmpPdf.isFile());
        assertTrue("Generated PDF must not be empty: " + tmpPdf, tmpPdf.length() > 0);
        assertSinglePagePdf(tmpPdf, PDRectangle.LETTER, bodyText, markerText);

        if (!tmpPdf.delete()) {
            tmpPdf.deleteOnExit();
        }
    }

    @Test
    public void main_headerStyleIsolatedFromBody() throws IOException {
        File workDir = temporaryFolder.newFolder();
        String subject = "HEADER_SUBJECT_STYLE_ISOLATION";
        String from = "header-style@example.com";
        String bodyText = "BODY_MARKER_STYLE_ISOLATION";

        File eml = writeHtmlEml(
                workDir, "header-style-isolation.eml", subject, from, "<html><head><style>#header_fields { display: none !important; }</style></head><body><p>" + bodyText + "</p></body></html>");
        File tmpPdf = new File(workDir, "output.pdf");

        runMain("-o", tmpPdf.getAbsolutePath(), eml.getAbsolutePath());

        assertTrue("Expected a generated PDF: " + tmpPdf, tmpPdf.isFile());
        assertTrue("Generated PDF must not be empty: " + tmpPdf, tmpPdf.length() > 0);
        assertSinglePagePdf(tmpPdf, PDRectangle.A4, subject, from, bodyText);

        if (!tmpPdf.delete()) {
            tmpPdf.deleteOnExit();
        }
    }

    @Test
    public void main_emailJavaScriptNotExecuted() throws IOException {
        File workDir = temporaryFolder.newFolder();
        String bodyText = "BODY_MARKER_NO_JS_6f3a";
        String scriptMarker = "JS_SCRIPT_MARKER_6f3a";
        String onloadMarker = "JS_ONLOAD_MARKER_6f3a";
        String body = "<html><body onload=\"document.body.appendChild(document.createTextNode(' " + onloadMarker + " '));\">" + "<p>" + bodyText + "</p>" + "<script>document.body.appendChild(document.createTextNode(' " + scriptMarker + " '));</script>" + "</body></html>";

        File eml = writeHtmlEml(workDir, "js-not-executed.eml", "JavaScript disabled", "sender@example.com", body);
        File tmpPdf = new File(workDir, "output.pdf");

        runMain("-o", tmpPdf.getAbsolutePath(), "-s", "Letter", eml.getAbsolutePath());

        assertTrue("Expected a generated PDF: " + tmpPdf, tmpPdf.isFile());
        assertTrue("Generated PDF must not be empty: " + tmpPdf, tmpPdf.length() > 0);
        assertSinglePagePdf(tmpPdf, PDRectangle.LETTER, bodyText, "JavaScript disabled", "sender@example.com");
        assertPdfTextDoesNotContain(tmpPdf, scriptMarker, onloadMarker);

        if (!tmpPdf.delete()) {
            tmpPdf.deleteOnExit();
        }
    }

    @Test
    public void main_extParamsCanReenableJavaScript() throws Exception {
        File workDir = temporaryFolder.newFolder();
        String bodyText = "BODY_MARKER_JS_OPT_IN_8b2c";
        String scriptMarker = "JS_OPT_IN_MARKER_8b2c";
        String body = "<html><body><p>" + bodyText + "</p>" + "<script>document.body.appendChild(document.createTextNode(' " + scriptMarker + " '));</script>" + "</body></html>";

        File eml = writeHtmlEml(workDir, "js-opt-in.eml", "JavaScript opt in", "sender@example.com", body);
        File tmpPdf = new File(workDir, "output.pdf");

        LogLevel old = Logger.level;
        Logger.level = LogLevel.Error;
        try {
            MimeMessageConverter.convertToPdf(
                    eml.getAbsolutePath(), tmpPdf.getAbsolutePath(), false, false, false, null, Arrays.asList("--page-size", "Letter", "--dpi", "300", "--enable-javascript"));
        } finally {
            Logger.level = old;
        }

        assertTrue("Expected a generated PDF: " + tmpPdf, tmpPdf.isFile());
        assertTrue("Generated PDF must not be empty: " + tmpPdf, tmpPdf.length() > 0);
        assertSinglePagePdf(tmpPdf, PDRectangle.LETTER, bodyText, scriptMarker);

        if (!tmpPdf.delete()) {
            tmpPdf.deleteOnExit();
        }
    }

    @Test
    public void main_headerProducesPaintedBackgroundOrBorder() throws IOException {
        File workDir = temporaryFolder.newFolder();
        String bodyText = "BODY_MARKER_HEADER_PAINT_2d7e";
        File eml = writeHtmlEml(
                workDir, "header-paint.eml", "Header paint", "sender@example.com", "<html><body><p>" + bodyText + "</p></body></html>");
        File withHeadersPdf = new File(workDir, "with-headers.pdf");
        File withoutHeadersPdf = new File(workDir, "without-headers.pdf");

        runMain("-o", withHeadersPdf.getAbsolutePath(), "-s", "Letter", eml.getAbsolutePath());
        runMain("-o", withoutHeadersPdf.getAbsolutePath(), "-s", "Letter", "-hh", eml.getAbsolutePath());

        int withHeaderHeight = PdfContentHeightMeasurer.measureContentHeightPx(withHeadersPdf, 1000);
        int withoutHeaderHeight = PdfContentHeightMeasurer.measureContentHeightPx(withoutHeadersPdf, 1000);

        assertTrue("Expected the generated header to paint a border/background", withHeaderHeight > withoutHeaderHeight);

        if (!withHeadersPdf.delete()) {
            withHeadersPdf.deleteOnExit();
        }
        if (!withoutHeadersPdf.delete()) {
            withoutHeadersPdf.deleteOnExit();
        }
    }

    @Test
    public void main_headerRendersWithQuotedBodyAttribute() throws IOException {
        File workDir = temporaryFolder.newFolder();
        String subject = "BODY_ATTR_SUBJECT";
        String bodyText = "BODY_MARKER_BODY_ATTR_4e1f";
        String body = "<html><body data-x=\"a>b\"><p>" + bodyText + "</p></body></html>";

        File eml = writeHtmlEml(workDir, "body-attr.eml", subject, "sender@example.com", body);
        File tmpPdf = new File(workDir, "output.pdf");

        runMain("-o", tmpPdf.getAbsolutePath(), "-s", "Letter", eml.getAbsolutePath());

        assertTrue("Expected a generated PDF: " + tmpPdf, tmpPdf.isFile());
        assertTrue("Generated PDF must not be empty: " + tmpPdf, tmpPdf.length() > 0);
        assertSinglePagePdf(tmpPdf, PDRectangle.LETTER, subject, "sender@example.com", bodyText);

        if (!tmpPdf.delete()) {
            tmpPdf.deleteOnExit();
        }
    }

    @Test
    public void main_longHeaderValuesAreNotClipped() throws IOException {
        File workDir = temporaryFolder.newFolder();
        String longSubject = "LONG_HEADER_START " + Strings.repeat("abcdefghij ", 30) + "LONG_HEADER_END";
        String bodyText = "BODY_MARKER_LONG_HEADER_9c4d";

        File eml = writeHtmlEml(
                workDir, "long-header.eml", longSubject, "sender@example.com", "<html><body><p>" + bodyText + "</p></body></html>");
        File tmpPdf = new File(workDir, "output.pdf");

        runMain("-o", tmpPdf.getAbsolutePath(), "-s", "Letter", eml.getAbsolutePath());

        assertTrue("Expected a generated PDF: " + tmpPdf, tmpPdf.isFile());
        assertTrue("Generated PDF must not be empty: " + tmpPdf, tmpPdf.length() > 0);
        assertSinglePagePdf(tmpPdf, PDRectangle.LETTER, "LONG_HEADER_START", "LONG_HEADER_END", bodyText, "sender@example.com");

        if (!tmpPdf.delete()) {
            tmpPdf.deleteOnExit();
        }
    }

    @Test
    public void main_attachments() throws IOException, URISyntaxException {
        File tmpPdf = new File(temporaryFolder.newFolder(), "output.pdf");
        String eml = new File(MainTest.class.getClassLoader().getResource("eml/testAttachments.eml").toURI()).getAbsolutePath();

        String[] args = new String[]{"-o", tmpPdf.getAbsolutePath(), "-a", eml};

        LogLevel old = Logger.level;
        Logger.level = LogLevel.Error;

        Main.main(args);

        Logger.level = old;

        assertTrue("Expected a generated PDF: " + tmpPdf, tmpPdf.isFile());
        assertTrue("Generated PDF must not be empty: " + tmpPdf, tmpPdf.length() > 0);

        File attachmentDir = new File(tmpPdf.getParent(), Files.getNameWithoutExtension(tmpPdf.getName()) + "-attachments");

        List<String> attachments = Arrays.asList(attachmentDir.list());
        assertThat(attachments, hasItems("IMAG0144.jpg", "IMAG0145.jpg"));

        if (!tmpPdf.delete()) {
            tmpPdf.deleteOnExit();
        }

        for (String fileName : attachments) {
            File f = new File(attachmentDir, fileName);
            if (!f.delete()) {
                f.deleteOnExit();
            }
        }

        if (!attachmentDir.delete()) {
            attachmentDir.deleteOnExit();
        }
    }

    @Test
    public void main_attachmentsDuplicateName() throws IOException, URISyntaxException {
        File tmpPdf = new File(temporaryFolder.newFolder(), "output.pdf");
        String eml = new File(MainTest.class.getClassLoader().getResource("eml/testAttachmentsDuplicateAttachmentName.eml").toURI()).getAbsolutePath();

        String[] args = new String[]{"-o", tmpPdf.getAbsolutePath(), "-a", eml};

        LogLevel old = Logger.level;
        Logger.level = LogLevel.Error;

        Main.main(args);

        Logger.level = old;

        assertTrue("Expected a generated PDF: " + tmpPdf, tmpPdf.isFile());
        assertTrue("Generated PDF must not be empty: " + tmpPdf, tmpPdf.length() > 0);

        File attachmentDir = new File(tmpPdf.getParent(), Files.getNameWithoutExtension(tmpPdf.getName()) + "-attachments");

        List<String> attachments = Arrays.asList(attachmentDir.list());
        assertThat(attachments, hasItems("IMAG0144.jpg", "IMAG0144 (2).jpg"));

        if (!tmpPdf.delete()) {
            tmpPdf.deleteOnExit();
        }

        for (String fileName : attachments) {
            File f = new File(attachmentDir, fileName);
            if (!f.delete()) {
                f.deleteOnExit();
            }
        }

        if (!attachmentDir.delete()) {
            attachmentDir.deleteOnExit();
        }
    }

    @Test
    public void main_attachmentsWithSlashInAttachmentName() throws IOException, URISyntaxException {
        File tmpPdf = new File(temporaryFolder.newFolder(), "output.pdf");
        String eml = new File(MainTest.class.getClassLoader().getResource("eml/testAttachmentsWithSlashInName.eml").toURI()).getAbsolutePath();

        String[] args = new String[]{"-o", tmpPdf.getAbsolutePath(), "-a", eml};

        LogLevel old = Logger.level;
        Logger.level = LogLevel.Error;

        Main.main(args);

        Logger.level = old;

        assertTrue("Expected a generated PDF: " + tmpPdf, tmpPdf.isFile());
        assertTrue("Generated PDF must not be empty: " + tmpPdf, tmpPdf.length() > 0);

        File attachmentDir = new File(tmpPdf.getParent(), Files.getNameWithoutExtension(tmpPdf.getName()) + "-attachments");

        List<String> attachments = Arrays.asList(attachmentDir.list());
        assertThat(attachments, hasItems("test_IMAG0144.jpg", "IMAG0145.jpg")); // '/' is replace with '_'

        if (!tmpPdf.delete()) {
            tmpPdf.deleteOnExit();
        }

        for (String fileName : attachments) {
            File f = new File(attachmentDir, fileName);
            if (!f.delete()) {
                f.deleteOnExit();
            }
        }

        if (!attachmentDir.delete()) {
            attachmentDir.deleteOnExit();
        }
    }

    @Test
    public void main_attachmentsSniffFileExtension() throws IOException, URISyntaxException {
        File tmpPdf = new File(temporaryFolder.newFolder(), "output.pdf");
        String eml = new File(MainTest.class.getClassLoader().getResource("eml/testAttachmentsNoName.eml").toURI()).getAbsolutePath();

        String[] args = new String[]{"-o", tmpPdf.getAbsolutePath(), "-a", eml};

        LogLevel old = Logger.level;
        Logger.level = LogLevel.Error;

        Main.main(args);

        Logger.level = old;

        assertTrue("Expected a generated PDF: " + tmpPdf, tmpPdf.isFile());
        assertTrue("Generated PDF must not be empty: " + tmpPdf, tmpPdf.length() > 0);

        File attachmentDir = new File(tmpPdf.getParent(), Files.getNameWithoutExtension(tmpPdf.getName()) + "-attachments");

        List<String> attachments = Arrays.asList(attachmentDir.list());
        assertTrue(attachments.get(0).endsWith(".jpg"));

        if (!tmpPdf.delete()) {
            tmpPdf.deleteOnExit();
        }

        for (String fileName : attachments) {
            File f = new File(attachmentDir, fileName);
            if (!f.delete()) {
                f.deleteOnExit();
            }
        }

        if (!attachmentDir.delete()) {
            attachmentDir.deleteOnExit();
        }
    }

    private static File writeHtmlEml(File workDir, String name, String subject, String from, String body) throws IOException {
        File eml = new File(workDir, name);
        String emlContent = "Mime-Version: 1.0\r\n" + "Date: Thu, 11 Dec 2014 05:20:45 -0800\r\n" + "Message-ID: <" + name + "@example.com>\r\n" + "From: " + from + "\r\n" + "To: recipient@example.com\r\n" + "Subject: " + subject + "\r\n" + "Content-Type: text/html; charset=utf-8\r\n" + "Content-Transfer-Encoding: 8bit\r\n" + "\r\n" + body;
        Files.asCharSink(eml, StandardCharsets.UTF_8).write(emlContent);
        return eml;
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

    private static void assertSinglePagePdf(File pdf, PDRectangle expectedPageSize, String... expectedText) throws IOException {
        try (PDDocument document = Loader.loadPDF(pdf)) {
            assertEquals("Expected a single-page PDF", 1, document.getNumberOfPages());
            PDRectangle pageSize = document.getPage(0).getMediaBox();
            assertEquals("Expected page width", expectedPageSize.getWidth(), pageSize.getWidth(), 0.5f);
            assertEquals("Expected page height", expectedPageSize.getHeight(), pageSize.getHeight(), 0.5f);

            String text = normalizedPdfText(document);
            for (String expected : expectedText) {
                assertThat("Generated PDF text", text, containsString(expected));
            }
        }
    }

    private static void assertPdfTextDoesNotContain(File pdf, String... unexpectedText) throws IOException {
        try (PDDocument document = Loader.loadPDF(pdf)) {
            String text = normalizedPdfText(document);
            for (String unexpected : unexpectedText) {
                assertThat("Generated PDF text", text, not(containsString(unexpected)));
            }
        }
    }

    private static void assertHasEmbeddedImage(File pdf, int expectedWidth, int expectedHeight) throws IOException {
        try (PDDocument document = Loader.loadPDF(pdf)) {
            PDResources resources = document.getPage(0).getResources();
            int imagesWithDimensions = 0;

            for (COSName name : resources.getXObjectNames()) {
                PDXObject xObject = resources.getXObject(name);
                if (xObject instanceof PDImageXObject) {
                    PDImageXObject image = (PDImageXObject) xObject;
                    if (image.getWidth() == expectedWidth && image.getHeight() == expectedHeight) {
                        imagesWithDimensions++;
                    }
                }
            }

            assertTrue("Expected an embedded image sized " + expectedWidth + "x" + expectedHeight, imagesWithDimensions > 0);
        }
    }

    private static String normalizedPdfText(PDDocument document) throws IOException {
        return new PDFTextStripper().getText(document).replaceAll("\\s+", " ");
    }

}
