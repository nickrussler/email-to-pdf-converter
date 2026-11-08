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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.google.common.io.Files;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.Assume;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import render.chrome.ChromeRenderer;
import util.LogLevel;
import util.Logger;

/**
 * Chrome proxy forwarding tests.
 */
public class ChromeProxyTest {
    private static final byte[] ONE_PIXEL_PNG = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=");

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void forwardsRemoteResourcesThroughConfiguredProxy() throws Exception {
        try (LocalProxy proxy = new LocalProxy()) {
            String html = "<html><head><link rel=\"stylesheet\" href=\"http://remote.test/style.css\"></head><body>" + "<p>LOCAL_BODY_MARKER</p>" + "<iframe src=\"http://remote.test/frame.html\" style=\"width:400px;height:120px\"></iframe>" + "<img src=\"http://remote.test/image.png\" width=\"10\" height=\"10\">" + "</body></html>";
            File output = new File(temporaryFolder.newFolder(), "proxied.pdf");

            render(html, proxy.url(), output);

            String text = pdfText(output);
            assertThat(text, containsString("LOCAL_BODY_MARKER"));
            assertThat(text, containsString("REMOTE_IFRAME_MARKER"));
            assertTrue("Expected iframe request through proxy: " + proxy.paths(), proxy.paths().contains("/frame.html"));
            assertTrue("Expected stylesheet request through proxy: " + proxy.paths(), proxy.paths().contains("/style.css"));
            assertTrue("Expected image request through proxy: " + proxy.paths(), proxy.paths().contains("/image.png"));
            assertTrue("Expected HTTPS CONNECT through proxy", proxy.connectCount() > 0);
        }
    }

    @Test
    public void sendsHttpsResourceThroughProxyConnect() throws Exception {
        try (LocalProxy proxy = new LocalProxy()) {
            String html = "<html><body><p>HTTPS_CONNECT_MARKER</p><img src=\"https://remote.test/secure.png\"></body></html>";
            File output = new File(temporaryFolder.newFolder(), "https-connect.pdf");

            render(html, proxy.url(), output);

            assertThat(pdfText(output), containsString("HTTPS_CONNECT_MARKER"));
            assertTrue("Expected HTTPS CONNECT through proxy", proxy.connectCount() > 0);
        }
    }

    @Test
    public void proxyArgumentIsPassedForFullChromeAndHeadlessShell() throws Exception {
        Assume.assumeFalse(System.getProperty("os.name").toLowerCase().contains("win"));

        List<String> fullChrome = launchArgs("google-chrome", "http://proxy.example.com:3128");
        assertTrue(fullChrome.contains("--headless=new"));
        assertTrue(fullChrome.contains("--proxy-server=http://proxy.example.com:3128"));

        List<String> headlessShell = launchArgs("chrome-headless-shell", "socks5://proxy.example.com:1080");
        assertFalse(headlessShell.contains("--headless=new"));
        assertTrue(headlessShell.contains("--proxy-server=socks5://proxy.example.com:1080"));
    }

    @Test
    public void noProxyAddsNoProxyArgument() throws Exception {
        Assume.assumeFalse(System.getProperty("os.name").toLowerCase().contains("win"));

        List<String> args = launchArgs("google-chrome", null);
        assertFalse(args.stream().anyMatch(value -> value.startsWith("--proxy-server=")));
    }

    @Test
    public void rejectsProxyCredentialsBeforeLaunchingBrowser() throws Exception {
        Assume.assumeFalse(System.getProperty("os.name").toLowerCase().contains("win"));

        File args = new File(temporaryFolder.newFolder(), "args.txt");
        File browser = fakeBrowser("google-chrome", args);
        RenderOptions options = options(browser.getAbsolutePath(), "http://user:password@proxy.example.com:3128");
        try {
            render("<html><body>test</body></html>", options, new File(temporaryFolder.newFolder(), "credentials.pdf"));
            fail("Expected proxy credentials to be rejected");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), containsString("Proxy authentication is not supported"));
        }
        assertFalse("Browser must not be launched with invalid proxy settings", args.exists());
    }

    private void render(String html, String proxy, File output) throws Exception {
        render(html, options(null, proxy), output);
    }

    private void render(String html, RenderOptions options, File output) throws Exception {
        LogLevel old = Logger.level;
        Logger.level = LogLevel.Error;
        try {
            new ChromeRenderer(options).render(html, StandardCharsets.UTF_8, output);
        } finally {
            Logger.level = old;
        }
    }

    private RenderOptions options(String executable, String proxy) {
        return new RenderOptions(
                RenderOptions.Renderer.CHROME, executable, proxy, "A4", "300", false, null, RenderOptions.BrowserDownload.NEVER, null);
    }

    private List<String> launchArgs(String executableName, String proxy) throws Exception {
        File args = new File(temporaryFolder.newFolder(), "args.txt");
        File browser = fakeBrowser(executableName, args);
        try {
            render("<html><body>test</body></html>", options(browser.getAbsolutePath(), proxy), new File(temporaryFolder.newFolder(), "out.pdf"));
            fail("Expected the fake browser to exit");
        } catch (IOException expected) {
            // The fake browser exits before DevTools becomes available.
        }
        return java.nio.file.Files.readAllLines(args.toPath(), StandardCharsets.UTF_8);
    }

    private File fakeBrowser(String name, File args) throws Exception {
        File directory = temporaryFolder.newFolder(name + "-dir");
        File browser = new File(directory, name);
        String script = "#!/bin/sh\nprintf '%s\\n' \"$@\" > '" + args.getAbsolutePath() + "'\nexit 1\n";
        Files.asCharSink(browser, StandardCharsets.UTF_8).write(script);
        assertTrue(browser.setExecutable(true));
        return browser;
    }

    private static String pdfText(File pdf) throws IOException {
        try (PDDocument document = Loader.loadPDF(pdf)) {
            return new PDFTextStripper().getText(document).replaceAll("\\s+", " ");
        }
    }

    private static final class LocalProxy implements AutoCloseable {
        private final ServerSocket server;
        private final ExecutorService executor = Executors.newCachedThreadPool();
        private final Thread acceptThread;
        private final List<String> paths = Collections.synchronizedList(new ArrayList<String>());
        private final AtomicInteger connectCount = new AtomicInteger();

        private LocalProxy() throws IOException {
            server = new ServerSocket(0, 50, InetAddress.getLoopbackAddress());
            acceptThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    acceptLoop();
                }
            });
            acceptThread.setDaemon(true);
            acceptThread.start();
        }

        private String url() {
            return "http://127.0.0.1:" + server.getLocalPort();
        }

        private List<String> paths() {
            synchronized (paths) {
                return new ArrayList<>(paths);
            }
        }

        private int connectCount() {
            return connectCount.get();
        }

        private void acceptLoop() {
            while (!server.isClosed()) {
                try {
                    final Socket socket = server.accept();
                    executor.submit(new Runnable() {
                        @Override
                        public void run() {
                            handle(socket);
                        }
                    });
                } catch (IOException e) {
                    return;
                }
            }
        }

        private void handle(Socket socket) {
            try (Socket connection = socket) {
                connection.setSoTimeout(5000);
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.ISO_8859_1));
                String requestLine = reader.readLine();
                if (requestLine == null) {
                    return;
                }
                String[] parts = requestLine.split(" ");
                if (parts.length < 2) {
                    return;
                }
                while (true) {
                    String header = reader.readLine();
                    if (header == null || header.isEmpty()) {
                        break;
                    }
                }

                if ("CONNECT".equals(parts[0])) {
                    connectCount.incrementAndGet();
                    write(connection.getOutputStream(), "HTTP/1.1 200 Connection established\r\n\r\n".getBytes(StandardCharsets.ISO_8859_1));
                    return;
                }

                String path = URI.create(parts[1]).getPath();
                paths.add(path);
                byte[] body;
                String contentType;
                if ("/frame.html".equals(path)) {
                    body = "<html><body><p>REMOTE_IFRAME_MARKER</p></body></html>".getBytes(StandardCharsets.UTF_8);
                    contentType = "text/html; charset=utf-8";
                } else if ("/style.css".equals(path)) {
                    body = "body { color: #112233; }".getBytes(StandardCharsets.UTF_8);
                    contentType = "text/css";
                } else if ("/image.png".equals(path)) {
                    body = ONE_PIXEL_PNG;
                    contentType = "image/png";
                } else {
                    body = "not found".getBytes(StandardCharsets.UTF_8);
                    contentType = "text/plain";
                }
                String headers = "HTTP/1.1 " + ("/image.png".equals(path) || "/frame.html".equals(path) || "/style.css".equals(path) ? "200 OK" : "404 Not Found") + "\r\nContent-Type: " + contentType + "\r\nContent-Length: " + body.length + "\r\nConnection: close\r\n\r\n";
                OutputStream output = connection.getOutputStream();
                write(output, headers.getBytes(StandardCharsets.ISO_8859_1));
                write(output, body);
            } catch (IOException ignored) {
                // best effort test proxy
            }
        }

        private static void write(OutputStream output, byte[] bytes) throws IOException {
            output.write(bytes);
            output.flush();
        }

        @Override
        public void close() throws Exception {
            server.close();
            executor.shutdownNow();
        }
    }
}
