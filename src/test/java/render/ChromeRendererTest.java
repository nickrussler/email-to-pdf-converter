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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.Assume;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import render.chrome.ChromeRenderer;

/**
 * Chrome renderer security and startup tests.
 */
public class ChromeRendererTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void crossSiteIframeMustNotExecuteJavaScriptWhenDisabled() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("0.0.0.0", 0), 0);
        server.createContext("/parent.html", htmlHandler("<html><body><p>PARENT_MARKER</p><iframe src=\"http://localhost:" + server.getAddress().getPort() + "/child.html\" style=\"width:400px;height:200px\"></iframe></body></html>"));
        server.createContext("/child.html", htmlHandler("<html><body><p>CHILD_STATIC_MARKER</p><script>document.body.appendChild(document.createTextNode(' CHILD_SCRIPT_MARKER '));</script></body></html>"));
        server.start();
        try {
            String html = "<html><body><p>TOP_MARKER</p><iframe src=\"http://127.0.0.1:" + server.getAddress().getPort() + "/parent.html\" style=\"width:600px;height:300px\"></iframe></body></html>";
            File output = new File(temporaryFolder.newFolder(), "iframe.pdf");
            RenderOptions options = new RenderOptions(RenderOptions.Renderer.CHROME, null, null, "A4", "300", false, null);
            new ChromeRenderer(options).render(html, StandardCharsets.UTF_8, output);

            String text = pdfText(output);
            assertThat(text, containsString("CHILD_STATIC_MARKER"));
            assertFalse("Cross-site iframe executed JavaScript while disabled: " + text, text.contains("CHILD_SCRIPT_MARKER"));
        } finally {
            server.stop(0);
        }
    }

    @Test
    public void devToolsDiscoveryIgnoresGlobalProxySelector() throws Exception {
        ProxySelector original = ProxySelector.getDefault();
        final ServerSocket proxyServer = new ServerSocket(0, 50, InetAddress.getLoopbackAddress());
        final AtomicInteger proxyConnections = new AtomicInteger();
        Thread proxyThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (!proxyServer.isClosed()) {
                    try (Socket ignored = proxyServer.accept()) {
                        proxyConnections.incrementAndGet();
                    } catch (IOException ignored) {
                        return;
                    }
                }
            }
        });
        proxyThread.setDaemon(true);
        proxyThread.start();

        ProxySelector.setDefault(new ProxySelector() {
            @Override
            public List<Proxy> select(URI uri) {
                return Collections.singletonList(new Proxy(
                        Proxy.Type.HTTP, new InetSocketAddress(InetAddress.getLoopbackAddress(), proxyServer.getLocalPort())));
            }

            @Override
            public void connectFailed(URI uri, SocketAddress address, IOException exception) {
            }
        });

        try {
            File output = new File(temporaryFolder.newFolder(), "proxy-bypass.pdf");
            RenderOptions options = new RenderOptions(
                    RenderOptions.Renderer.CHROME, System.getenv("CHROME_PATH"), null, "A4", "300", false, null);
            new ChromeRenderer(options).render(
                    "<html><body><p>PROXY_BYPASS_MARKER</p></body></html>", StandardCharsets.UTF_8, output);

            assertThat(pdfText(output), containsString("PROXY_BYPASS_MARKER"));
            assertEquals("DevTools discovery must not connect through the global proxy", 0, proxyConnections.get());
        } finally {
            ProxySelector.setDefault(original);
            proxyServer.close();
        }
    }

    @Test
    public void startupFailureIncludesBrowserDiagnostics() throws Exception {
        Assume.assumeFalse("POSIX shell test", System.getProperty("os.name").toLowerCase().contains("win"));

        File fakeChrome = new File(temporaryFolder.newFolder(), "fake-chrome");
        com.google.common.io.Files.asCharSink(fakeChrome, StandardCharsets.UTF_8).write("#!/bin/sh\necho STARTUP_DIAGNOSTIC_MARKER >&2\nexit 1\n");
        assertTrue(fakeChrome.setExecutable(true));

        RenderOptions options = new RenderOptions(RenderOptions.Renderer.CHROME, fakeChrome.getAbsolutePath(), null, "A4", "300", false, null);
        File output = new File(temporaryFolder.newFolder(), "out.pdf");
        try {
            new ChromeRenderer(options).render("<html><body>test</body></html>", StandardCharsets.UTF_8, output);
            fail("Expected startup failure");
        } catch (IOException e) {
            assertThat(e.getMessage(), containsString("STARTUP_DIAGNOSTIC_MARKER"));
        }
    }

    private static HttpHandler htmlHandler(final String html) {
        return new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                byte[] body = html.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
                exchange.sendResponseHeaders(200, body.length);
                try (OutputStream output = exchange.getResponseBody()) {
                    output.write(body);
                }
            }
        };
    }

    private static String pdfText(File pdf) throws IOException {
        try (PDDocument document = Loader.loadPDF(pdf)) {
            return new PDFTextStripper().getText(document).replaceAll("\\s+", " ");
        }
    }
}
