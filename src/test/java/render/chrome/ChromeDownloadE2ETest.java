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

import cli.Main;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.Assume;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import util.LogLevel;
import util.Logger;

/**
 * Downloads a Chrome headless shell and renders with it. Enabled explicitly in CI to keep normal test runs offline.
 */
public class ChromeDownloadE2ETest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void rendersWithDownloadedHeadlessShellAndReusesCache() throws Exception {
        Assume.assumeTrue("Set CHROME_DOWNLOAD_E2E=1 to run the browser download test", "1".equals(System.getenv("CHROME_DOWNLOAD_E2E")));

        Path cache = cacheDirectory();
        File email = resource("eml/testHtml.eml");
        File firstOutput = new File(temporaryFolder.newFolder(), "downloaded-first.pdf");
        File secondOutput = new File(temporaryFolder.newFolder(), "downloaded-second.pdf");

        convert(email, firstOutput, cache);
        assertDownloadedShell(cache);
        assertRendered(firstOutput);

        convert(email, secondOutput, cache);
        assertRendered(secondOutput);
    }

    private void convert(File email, File output, Path cache) {
        LogLevel oldLevel = Logger.level;
        Logger.level = LogLevel.Error;
        try {
            Main.main(new String[]{"--renderer", "chrome", "--browser-download", "once", "--browser-cache-dir", cache.toAbsolutePath().toString(), "-o", output.getAbsolutePath(), email.getAbsolutePath()});
        } finally {
            Logger.level = oldLevel;
        }
        assertTrue("Expected a generated PDF: " + output, output.isFile() && output.length() > 0);
    }

    private static void assertRendered(File output) throws Exception {
        try (PDDocument document = Loader.loadPDF(output)) {
            assertEquals(595.92f, document.getPage(0).getMediaBox().getWidth(), 1.0f);
            assertEquals(841.92f, document.getPage(0).getMediaBox().getHeight(), 1.0f);
            String text = new PDFTextStripper().getText(document).replaceAll("\\s+", " ");
            assertThat(text, containsString("Testing Manuel Lemos' MIME E-mail composing and sending PHP class: HTML message"));
            assertThat(text, containsString("Hello Manuel,"));
        }
    }

    private static void assertDownloadedShell(Path cache) throws Exception {
        assertTrue("Expected completed browser cache in " + cache, hasFile(cache, ChromeBrowserProvisioner.COMPLETE_MARKER));
        assertTrue("Expected extracted chrome-headless-shell in " + cache, hasFile(cache, "chrome-headless-shell"));
    }

    private static boolean hasFile(Path root, String fileName) throws Exception {
        try (Stream<Path> paths = Files.walk(root)) {
            return paths.anyMatch(path -> Files.isRegularFile(path) && fileName.equals(path.getFileName().toString()));
        }
    }

    private Path cacheDirectory() throws Exception {
        String configured = System.getenv("CHROME_DOWNLOAD_CACHE");
        if (configured != null && !configured.trim().isEmpty()) {
            Path cache = Paths.get(configured.trim());
            Files.createDirectories(cache);
            return cache;
        }
        return temporaryFolder.newFolder("chrome-cache").toPath();
    }

    private static File resource(String name) throws Exception {
        return new File(ChromeDownloadE2ETest.class.getClassLoader().getResource(name).toURI());
    }
}
