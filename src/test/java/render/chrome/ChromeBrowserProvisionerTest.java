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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.rules.TemporaryFolder;
import render.RenderOptions;

public class ChromeBrowserProvisionerTest {
    private static final String VERSION = "157.0.8103.38";
    private static final String NEWER_VERSION = "158.0.8103.38";

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Rule
    public EnvironmentVariables environment = new EnvironmentVariables();

    private HttpServer server;
    private Path cacheRoot;
    private String metadataJson;
    private byte[] archive;
    private final AtomicInteger metadataRequests = new AtomicInteger();
    private final AtomicInteger artifactRequests = new AtomicInteger();

    @Before
    public void setUp() throws Exception {
        cacheRoot = temporaryFolder.newFolder("cache").toPath();
        archive = zipWithExecutable("chrome-headless-shell-linux64/chrome-headless-shell");
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/metadata.json", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                metadataRequests.incrementAndGet();
                respond(exchange, metadataJson.getBytes(StandardCharsets.UTF_8));
            }
        });
        server.createContext("/chrome.zip", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                artifactRequests.incrementAndGet();
                respond(exchange, archive);
            }
        });
        server.start();
        metadataJson = metadata(VERSION);
    }

    @After
    public void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    public void autoUsesInstalledBrowserWithoutNetwork() throws Exception {
        File installed = executable("installed-chrome");
        environment.set("CHROME_PATH", installed.getAbsolutePath());
        environment.clear("CHROME_BIN", "GOOGLE_CHROME_BIN", "CHROMIUM_PATH");

        ChromeBrowserProvisioner.Browser browser = provisioner(options(RenderOptions.BrowserDownload.AUTO)).resolve();

        assertEquals(installed.getAbsolutePath(), browser.getExecutable());
        assertEquals(0, metadataRequests.get());
        assertEquals(0, artifactRequests.get());
    }

    @Test
    public void neverUsesCachedBrowserWithoutNetwork() throws Exception {
        Path cached = cacheExecutable(VERSION);
        environment.set("PATH", temporaryFolder.newFolder("empty-path-never").getAbsolutePath());
        environment.clear("CHROME_PATH", "CHROME_BIN", "GOOGLE_CHROME_BIN", "CHROMIUM_PATH");

        ChromeBrowserProvisioner.Browser browser = provisioner(options(RenderOptions.BrowserDownload.NEVER)).resolve();

        assertEquals(cached.toAbsolutePath().toString(), browser.getExecutable());
        assertEquals(VERSION, browser.getVersion());
        assertEquals(0, metadataRequests.get());
    }

    @Test
    public void neverFailsWithoutBrowserOrCacheAndDoesNotUseNetwork() throws Exception {
        environment.set("PATH", temporaryFolder.newFolder("empty-path").getAbsolutePath());
        environment.clear("CHROME_PATH", "CHROME_BIN", "GOOGLE_CHROME_BIN", "CHROMIUM_PATH");

        try {
            provisioner(options(RenderOptions.BrowserDownload.NEVER)).resolve();
            fail("Expected a missing browser error");
        } catch (IllegalStateException e) {
            assertThat(e.getMessage(), containsString("browser downloads are disabled"));
        }

        assertEquals(0, metadataRequests.get());
        assertEquals(0, artifactRequests.get());
    }

    @Test
    public void autoDownloadsWhenNoInstalledBrowserExists() throws Exception {
        environment.set("PATH", temporaryFolder.newFolder("empty-path-auto").getAbsolutePath());
        environment.clear("CHROME_PATH", "CHROME_BIN", "GOOGLE_CHROME_BIN", "CHROMIUM_PATH");

        ChromeBrowserProvisioner.Browser browser = provisioner(options(RenderOptions.BrowserDownload.AUTO)).resolve();

        assertTrue(browser.isHeadlessShell());
        assertEquals(VERSION, browser.getVersion());
        assertEquals(1, metadataRequests.get());
        assertEquals(1, artifactRequests.get());
    }

    @Test
    public void onceIgnoresInstalledBrowserAndDownloadsHeadlessShell() throws Exception {
        File installed = executable("installed-chrome");
        environment.set("CHROME_PATH", installed.getAbsolutePath());
        environment.clear("CHROME_BIN", "GOOGLE_CHROME_BIN", "CHROMIUM_PATH");

        ChromeBrowserProvisioner.Browser browser = provisioner(options(RenderOptions.BrowserDownload.ONCE)).resolve();

        assertFalse(installed.getAbsolutePath().equals(browser.getExecutable()));
        assertTrue(browser.isHeadlessShell());
        assertEquals(VERSION, browser.getVersion());
        assertEquals(1, metadataRequests.get());
        assertEquals(1, artifactRequests.get());
        assertTrue(Files.isRegularFile(Paths.get(browser.getExecutable())));
        assertTrue(Files.isRegularFile(cachePlatformDirectory(VERSION).resolve(ChromeBrowserProvisioner.COMPLETE_MARKER)));
    }

    @Test
    public void onceReusesCachedBrowserWithoutMetadataRequest() throws Exception {
        Path cached = cacheExecutable(VERSION);
        environment.clear("CHROME_PATH", "CHROME_BIN", "GOOGLE_CHROME_BIN", "CHROMIUM_PATH");

        ChromeBrowserProvisioner.Browser browser = provisioner(options(RenderOptions.BrowserDownload.ONCE)).resolve();

        assertEquals(cached.toAbsolutePath().toString(), browser.getExecutable());
        assertEquals(0, metadataRequests.get());
        assertEquals(0, artifactRequests.get());
    }

    @Test
    public void redownloadsCacheWithoutCompletionMarker() throws Exception {
        Path platformDirectory = cachePlatformDirectory(VERSION);
        Path executable = platformDirectory.resolve("chrome-headless-shell-linux64/chrome-headless-shell");
        Files.createDirectories(executable.getParent());
        Files.write(executable, "#!/bin/sh\nexit 0\n".getBytes(StandardCharsets.UTF_8));
        environment.clear("CHROME_PATH", "CHROME_BIN", "GOOGLE_CHROME_BIN", "CHROMIUM_PATH");

        ChromeBrowserProvisioner.Browser browser = provisioner(options(RenderOptions.BrowserDownload.ONCE)).resolve();

        assertTrue(browser.isHeadlessShell());
        assertEquals(1, artifactRequests.get());
        assertTrue(Files.isRegularFile(platformDirectory.resolve(ChromeBrowserProvisioner.COMPLETE_MARKER)));
    }

    @Test
    public void latestDownloadsNewerCachedVersion() throws Exception {
        cacheExecutable(VERSION);
        metadataJson = metadata(NEWER_VERSION);
        environment.clear("CHROME_PATH", "CHROME_BIN", "GOOGLE_CHROME_BIN", "CHROMIUM_PATH");

        ChromeBrowserProvisioner.Browser browser = provisioner(options(RenderOptions.BrowserDownload.LATEST)).resolve();

        assertEquals(NEWER_VERSION, browser.getVersion());
        assertEquals(1, metadataRequests.get());
        assertEquals(1, artifactRequests.get());
        assertTrue(browser.getExecutable().contains(NEWER_VERSION));
    }

    @Test
    public void latestReusesCachedLatestWithoutArtifactDownload() throws Exception {
        Path cached = cacheExecutable(VERSION);
        environment.clear("CHROME_PATH", "CHROME_BIN", "GOOGLE_CHROME_BIN", "CHROMIUM_PATH");

        ChromeBrowserProvisioner.Browser browser = provisioner(options(RenderOptions.BrowserDownload.LATEST)).resolve();

        assertEquals(cached.toAbsolutePath().toString(), browser.getExecutable());
        assertEquals(1, metadataRequests.get());
        assertEquals(0, artifactRequests.get());
    }

    @Test
    public void explicitPathWinsOverDownloads() throws Exception {
        File installed = executable("explicit-chrome");
        ChromeBrowserProvisioner.Browser browser = provisioner(options(RenderOptions.BrowserDownload.LATEST, installed.getAbsolutePath())).resolve();

        assertEquals(installed.getAbsolutePath(), browser.getExecutable());
        assertEquals(0, metadataRequests.get());
    }

    @Test
    public void invalidExplicitPathDoesNotDownload() throws Exception {
        try {
            provisioner(options(RenderOptions.BrowserDownload.AUTO, new File(temporaryFolder.getRoot(), "missing-chrome").getAbsolutePath())).resolve();
            fail("Expected a missing executable error");
        } catch (IllegalStateException e) {
            assertThat(e.getMessage(), containsString("not found"));
        }

        assertEquals(0, metadataRequests.get());
    }

    @Test
    public void concurrentAcquisitionDownloadsOnlyOnce() throws Exception {
        environment.clear("CHROME_PATH", "CHROME_BIN", "GOOGLE_CHROME_BIN", "CHROMIUM_PATH");
        final CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<ChromeBrowserProvisioner.Browser> first = executor.submit(() -> {
                start.await();
                return provisioner(options(RenderOptions.BrowserDownload.ONCE)).resolve();
            });
            Future<ChromeBrowserProvisioner.Browser> second = executor.submit(() -> {
                start.await();
                return provisioner(options(RenderOptions.BrowserDownload.ONCE)).resolve();
            });
            start.countDown();

            assertEquals(first.get(60, TimeUnit.SECONDS).getExecutable(), second.get(60, TimeUnit.SECONDS).getExecutable());
            assertEquals(1, artifactRequests.get());
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    public void rejectsArchivePathsOutsideExtractionDirectory() throws Exception {
        archive = zipWithContent("../escape-marker", "escape");
        environment.clear("CHROME_PATH", "CHROME_BIN", "GOOGLE_CHROME_BIN", "CHROMIUM_PATH");

        try {
            provisioner(options(RenderOptions.BrowserDownload.ONCE)).resolve();
            fail("Expected an invalid archive path error");
        } catch (IOException e) {
            assertThat(e.getMessage(), containsString("invalid path"));
        }

        assertFalse(Files.exists(cacheRoot.resolve(ChromeBrowserProvisioner.CACHE_DIR_NAME).resolve("escape-marker")));
    }

    @Test
    public void rejectsUnsupportedHostArchitecture() {
        try {
            ChromeBrowserProvisioner.platformFor("Linux", "sparc");
            fail("Expected an unsupported platform error");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), containsString("Unsupported platform"));
        }
    }

    @Test
    public void parsesHttpAndSocksProxySettings() {
        Proxy http = ChromeBrowserProvisioner.parseProxy("http://127.0.0.1:3128");
        assertEquals(Proxy.Type.HTTP, http.type());
        assertEquals(new InetSocketAddress("127.0.0.1", 3128), http.address());

        Proxy socks = ChromeBrowserProvisioner.parseProxy("socks5://127.0.0.1:1080");
        assertEquals(Proxy.Type.SOCKS, socks.type());
        assertEquals(new InetSocketAddress("127.0.0.1", 1080), socks.address());
    }

    @Test
    public void comparesVersionsNumerically() {
        assertTrue(ChromeBrowserProvisioner.compareVersions("157.0.8103.38", "157.0.8103.4") > 0);
        assertTrue(ChromeBrowserProvisioner.compareVersions("156.0.8103.38", "157.0.8103.38") < 0);
        assertEquals(0, ChromeBrowserProvisioner.compareVersions("157.0.8103.38", "157.0.8103.38"));
    }

    private ChromeBrowserProvisioner provisioner(RenderOptions options) {
        return new ChromeBrowserProvisioner(options, metadataUrl(), cacheRoot, ChromeBrowserProvisioner.Platform.LINUX64);
    }

    private RenderOptions options(RenderOptions.BrowserDownload mode) {
        return options(mode, null);
    }

    private RenderOptions options(RenderOptions.BrowserDownload mode, String executable) {
        return new RenderOptions(
                RenderOptions.Renderer.CHROME, executable, null, "A4", "300", false, null, mode, cacheRoot.toAbsolutePath().toString());
    }

    private File executable(String name) throws Exception {
        File file = new File(temporaryFolder.newFolder(name + "-dir"), name);
        Files.write(file.toPath(), "#!/bin/sh\nexit 0\n".getBytes(StandardCharsets.UTF_8));
        assertTrue(file.setExecutable(true));
        return file;
    }

    private Path cacheExecutable(String version) throws IOException {
        Path platformDirectory = cachePlatformDirectory(version);
        Path executable = platformDirectory.resolve("chrome-headless-shell-linux64/chrome-headless-shell");
        Files.createDirectories(executable.getParent());
        Files.write(executable, "#!/bin/sh\nexit 0\n".getBytes(StandardCharsets.UTF_8));
        Files.write(platformDirectory.resolve(ChromeBrowserProvisioner.COMPLETE_MARKER), version.getBytes(StandardCharsets.UTF_8));
        return executable;
    }

    private Path cachePlatformDirectory(String version) {
        return cacheRoot.resolve(ChromeBrowserProvisioner.CACHE_DIR_NAME).resolve(version).resolve("linux64");
    }

    private String metadataUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort() + "/metadata.json";
    }

    private String metadata(String version) {
        return "{\"channels\":{\"Stable\":{\"version\":\"" + version + "\",\"downloads\":{\"chrome-headless-shell\":[{\"platform\":\"linux64\",\"url\":\"" + "http://127.0.0.1:" + server.getAddress().getPort() + "/chrome.zip\"}]}}}}";
    }

    private static byte[] zipWithExecutable(String executablePath) throws IOException {
        return zipWithContent(executablePath, "#!/bin/sh\nexit 0\n");
    }

    private static byte[] zipWithContent(String entryPath, String content) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(output)) {
            zip.putNextEntry(new ZipEntry(entryPath));
            zip.write(content.getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }
        return output.toByteArray();
    }

    private static void respond(HttpExchange exchange, byte[] body) throws IOException {
        exchange.sendResponseHeaders(200, body.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(body);
        }
    }
}
