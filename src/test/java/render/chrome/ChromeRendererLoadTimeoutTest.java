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

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import render.RenderOptions;

public class ChromeRendererLoadTimeoutTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void rendersLoadedContentWhenAnExternalResourceHangs() throws Exception {
        final CountDownLatch release = new CountDownLatch(1);
        final ExecutorService executor = Executors.newCachedThreadPool();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.setExecutor(executor);
        server.createContext("/hang.png", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                try {
                    release.await(10, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });
        server.start();

        try {
            String html = "<html><body><p>BEFORE_HANG</p><img src=\"http://127.0.0.1:" + server.getAddress().getPort() + "/hang.png\"><p>AFTER_HANG</p></body></html>";
            File output = new File(temporaryFolder.newFolder(), "hang.pdf");
            RenderOptions options = new RenderOptions(
                    RenderOptions.Renderer.CHROME, System.getenv("CHROME_PATH"), null, "A4", "300", false, null);
            new ChromeRenderer(options, 500).render(html, StandardCharsets.UTF_8, output);

            try (PDDocument document = Loader.loadPDF(output)) {
                String text = new PDFTextStripper().getText(document).replaceAll("\\s+", " ");
                assertThat(text, containsString("BEFORE_HANG"));
                assertThat(text, containsString("AFTER_HANG"));
            }
        } finally {
            release.countDown();
            server.stop(0);
            executor.shutdownNow();
        }
    }
}
