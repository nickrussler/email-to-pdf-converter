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

package render.chrome;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.google.common.io.Files;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import render.RenderOptions;

public class ChromeStylesheetTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void stylesheetChangesPrintBackground() throws Exception {
        File output = temporaryFolder.newFile("background.pdf");
        String css = "html, body { background-color: rgb(255, 0, 0) !important; } @page { margin: 0 !important; }";

        new ChromeRenderer(options(RenderOptions.Orientation.PORTRAIT, css)).render(
                "<html><body><p>STYLESHEET_MARKER</p></body></html>", StandardCharsets.UTF_8, output);

        try (PDDocument document = Loader.loadPDF(output)) {
            BufferedImage image = new PDFRenderer(document).renderImageWithDPI(0, 20);
            int pixel = image.getRGB(image.getWidth() / 2, image.getHeight() / 2);
            assertEquals(0xFF0000, pixel & 0xFFFFFF);
        }
    }

    @Test
    public void documentedStylesheetAddsLandscapeToAppliedPortraitRule() throws Exception {
        File output = temporaryFolder.newFile("landscape.pdf");
        String html = "<html><head><style>@page WordSection1 { size: 8.5in 11in; } body { page: WordSection1; }</style></head>" + "<body><p>DOCUMENTED_STYLESHEET_MARKER</p></body></html>";

        new ChromeRenderer(options(RenderOptions.Orientation.LANDSCAPE, "@page { size: auto !important; }")).render(
                html, StandardCharsets.UTF_8, output);

        try (PDDocument document = Loader.loadPDF(output)) {
            assertTrue(document.getPage(0).getMediaBox().getWidth() > document.getPage(0).getMediaBox().getHeight());
        }
    }

    @Test
    public void conflictingImportantPageRuleFailsLandscapeValidation() throws Exception {
        File output = temporaryFolder.newFile("conflict.pdf");
        Files.asCharSink(output, StandardCharsets.UTF_8).write("ORIGINAL_CONTENT");
        String html = "<html><head><style>@page WordSection1 { size: 8.5in 11in !important; } body { page: WordSection1; }</style></head>" + "<body><p>CONFLICT_MARKER</p></body></html>";

        try {
            new ChromeRenderer(options(RenderOptions.Orientation.LANDSCAPE, "@page { size: auto !important; }")).render(
                    html, StandardCharsets.UTF_8, output);
            fail("Expected landscape validation to fail");
        } catch (IOException e) {
            assertThat(e.getMessage(), containsString("Landscape was requested"));
            assertThat(e.getMessage(), containsString("page 1"));
        }

        assertEquals("ORIGINAL_CONTENT", Files.asCharSource(output, StandardCharsets.UTF_8).read());
    }

    private static RenderOptions options(RenderOptions.Orientation orientation, String stylesheet) {
        return new RenderOptions(
                RenderOptions.Renderer.CHROME, null, null, "A4", "300", false, null, RenderOptions.BrowserDownload.AUTO, null, orientation, stylesheet);
    }
}
