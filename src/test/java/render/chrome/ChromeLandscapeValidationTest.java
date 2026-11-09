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
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import render.RenderOptions;
import render.chrome.ChromeRenderer;

public class ChromeLandscapeValidationTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void acceptsRotatedLandscapePages() throws Exception {
        ChromeRenderer.validateLandscape(pdfWithPageRotation(90));
        ChromeRenderer.validateLandscape(pdfWithPageRotation(270));
    }

    @Test
    public void rejectsPortraitOutputAndPreservesExistingFile() throws Exception {
        File output = temporaryFolder.newFile("out.pdf");
        Files.asCharSink(output, StandardCharsets.UTF_8).write("ORIGINAL_CONTENT");

        RenderOptions options = new RenderOptions(
                RenderOptions.Renderer.CHROME, null, null, "A4", "300", false, null, RenderOptions.BrowserDownload.AUTO, null, RenderOptions.Orientation.LANDSCAPE);
        String html = "<html><head><style>@page WordSection1 { size: 8.5in 11in; } body { page: WordSection1; }</style></head>" + "<body><p>LANDSCAPE_FIXTURE_MARKER</p></body></html>";

        try {
            new ChromeRenderer(options).render(html, StandardCharsets.UTF_8, output);
            fail("Expected landscape validation to fail");
        } catch (IOException e) {
            assertThat(e.getMessage(), containsString("Landscape was requested"));
            assertThat(e.getMessage(), containsString("page 1"));
            assertThat(e.getMessage(), containsString("portrait"));
            assertThat(e.getMessage(), containsString("print CSS"));
        }

        assertEquals("ORIGINAL_CONTENT", Files.asCharSource(output, StandardCharsets.UTF_8).read());
    }

    private static byte[] pdfWithPageRotation(int rotation) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(new PDRectangle(595, 842));
            page.setRotation(rotation);
            document.addPage(page);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            document.save(output);
            return output.toByteArray();
        }
    }
}
