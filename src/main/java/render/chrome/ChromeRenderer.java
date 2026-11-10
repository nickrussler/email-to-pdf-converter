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

import com.google.common.io.BaseEncoding;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.ServerSocket;
import java.net.URI;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import mimeparser.PdfContentHeightMeasurer;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import render.PaperSize;
import render.PdfRenderer;
import render.RenderOptions;
import util.Logger;
import util.ProxyUrl;

/**
 * Renders HTML with an installed or downloaded Chrome browser using a minimal CDP client.
 */
public final class ChromeRenderer implements PdfRenderer {
    private static final long START_TIMEOUT_MS = 15000;
    private static final long BROWSER_EXIT_TIMEOUT_MS = 5000;
    private static final int HEADER_MIN_HEIGHT = 150;
    private static final int HEADER_PROBE_PAGE_HEIGHT_PX = 5000;
    private static final double CSS_PX_PER_INCH = 96.0;
    private static final double MM_PER_INCH = 25.4;
    private static final double PRINT_MARGIN_MM = 10.0;

    private final RenderOptions options;
    private final long loadTimeoutMs;
    private volatile ChromeBrowserProvisioner.Browser resolvedBrowser;

    public ChromeRenderer(RenderOptions options) {
        this(options, ChromeCdpClient.LOAD_TIMEOUT_MS);
    }

    ChromeRenderer(RenderOptions options, long loadTimeoutMs) {
        this.options = options;
        this.loadTimeoutMs = loadTimeoutMs;
    }

    @Override
    public void render(String html, Charset charset, File outputPdf) throws Exception {
        Logger.debug("Render Chrome document using %s encoding", charset.name());
        logUnsupportedOptions();

        PaperSize paperSize = PaperSize.fromName(options.getPageSize());
        renderHtml(html, outputPdf, paperSize.getWidthPoints(), paperSize.getHeightPoints(), PRINT_MARGIN_MM, true, options.getOrientation() == RenderOptions.Orientation.LANDSCAPE, options.getPrintStylesheet());
    }

    @Override
    public int measureHeaderHeight(String headerHtml, int widthPx) {
        File tmpProbe = null;
        try {
            tmpProbe = File.createTempFile("emailtopdf-header-probe", ".pdf");
            double widthPoints = widthPx * 72.0 / CSS_PX_PER_INCH;
            double heightPoints = HEADER_PROBE_PAGE_HEIGHT_PX * 72.0 / CSS_PX_PER_INCH;
            renderHtml(headerHtml, tmpProbe, widthPoints, heightPoints, 0, false, false, null);

            int measuredHeight = PdfContentHeightMeasurer.measureContentHeightPx(tmpProbe, widthPx);
            return Math.max(measuredHeight, HEADER_MIN_HEIGHT);
        } catch (Exception e) {
            Logger.error("Could not measure the header height with Chrome, using %dpx. Error: %s", HEADER_MIN_HEIGHT, e);
            return HEADER_MIN_HEIGHT;
        } finally {
            deleteTempFile(tmpProbe);
        }
    }

    private void renderHtml(String html, File outputPdf, double widthPoints, double heightPoints, double marginMm, boolean printBackground, boolean landscape, String stylesheet) throws Exception {
        String chromeProxy = ProxyUrl.normalizeForChrome(options.getProxy());
        ChromeBrowserProvisioner.Browser browserInfo = resolveBrowser();
        String executable = browserInfo.getExecutable();
        Path userDataDir = Files.createTempDirectory("emailtopdf-chrome-");
        File browserLog = File.createTempFile("emailtopdf-chrome-", ".log");
        Process browser = null;
        ChromeCdpClient cdp = null;
        try {
            int port = freePort();
            List<String> command = new ArrayList<>();
            command.add(executable);
            if (!browserInfo.isHeadlessShell()) {
                command.add("--headless=new");
            }
            command.addAll(Arrays.asList(
                    "--disable-gpu", "--no-first-run", "--no-default-browser-check", "--remote-debugging-port=" + port, "--user-data-dir=" + userDataDir.toAbsolutePath(), "about:blank"));
            if (chromeProxy != null) {
                command.add("--proxy-server=" + chromeProxy);
            }
            if (noSandbox()) {
                command.add(1, "--no-sandbox");
            }
            if (!options.isJavaScriptEnabled()) {
                // Emulation.setScriptExecutionDisabled only affects the attached page target.
                // This browser-wide setting also covers out-of-process iframes.
                command.add("--blink-settings=scriptEnabled=false");
            }

            ProcessBuilder builder = new ProcessBuilder(command);
            builder.redirectErrorStream(true);
            builder.redirectOutput(browserLog);
            browser = builder.start();

            String pageWebSocketUrl = waitForPageWebSocket(port, executable, browser, browserLog);
            cdp = new ChromeCdpClient(new URI(pageWebSocketUrl), loadTimeoutMs);
            cdp.connectBlocking(START_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            if (!cdp.isOpen()) {
                throw new IOException("Could not connect to Chrome DevTools: " + pageWebSocketUrl);
            }

            cdp.sendCommand("Page.enable");
            if (!options.isJavaScriptEnabled()) {
                // Defense in depth. This only affects the attached page target and does not cover
                // out-of-process iframes; the browser-wide --blink-settings guard above does.
                JsonObject disabled = new JsonObject();
                disabled.addProperty("value", true);
                cdp.sendCommand("Emulation.setScriptExecutionDisabled", disabled);
            }

            cdp.prepareLoad();
            JsonObject frameTree = cdp.sendCommand("Page.getFrameTree");
            String frameId = frameTree.getAsJsonObject("frameTree").getAsJsonObject("frame").get("id").getAsString();
            JsonObject document = new JsonObject();
            document.addProperty("frameId", frameId);
            document.addProperty("html", html);
            cdp.sendCommand("Page.setDocumentContent", document);
            if (!cdp.awaitLoad()) {
                Logger.info("Timed out waiting for Chrome to finish loading the document; rendering the loaded content.");
                cdp.sendCommand("Page.stopLoading");
            }
            applyStylesheet(cdp, frameId, stylesheet);

            JsonObject pdfOptions = new JsonObject();
            pdfOptions.addProperty("printBackground", printBackground);
            pdfOptions.addProperty("preferCSSPageSize", false);
            pdfOptions.addProperty("displayHeaderFooter", false);
            pdfOptions.addProperty("landscape", landscape);
            pdfOptions.addProperty("paperWidth", widthPoints / 72.0);
            pdfOptions.addProperty("paperHeight", heightPoints / 72.0);
            pdfOptions.addProperty("marginTop", marginMm / MM_PER_INCH);
            pdfOptions.addProperty("marginBottom", marginMm / MM_PER_INCH);
            pdfOptions.addProperty("marginLeft", marginMm / MM_PER_INCH);
            pdfOptions.addProperty("marginRight", marginMm / MM_PER_INCH);

            JsonObject result = cdp.sendCommand("Page.printToPDF", pdfOptions);
            if (!result.has("data")) {
                throw new IOException("Chrome returned no PDF data");
            }
            byte[] pdf = BaseEncoding.base64().decode(result.get("data").getAsString());
            if (landscape) {
                validateLandscape(pdf);
            }
            Files.write(outputPdf.toPath(), pdf);
        } finally {
            if (cdp != null) {
                cdp.close();
            }
            if (browser != null) {
                browser.destroy();
                if (!browser.waitFor(BROWSER_EXIT_TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
                    browser.destroyForcibly();
                }
            }
            deleteRecursively(userDataDir);
            deleteTempFile(browserLog);
        }
    }

    private static void applyStylesheet(ChromeCdpClient cdp, String frameId, String stylesheet) throws Exception {
        if (stylesheet == null || stylesheet.isEmpty()) {
            return;
        }

        cdp.sendCommand("DOM.enable");
        cdp.sendCommand("CSS.enable");

        JsonObject createParams = new JsonObject();
        createParams.addProperty("frameId", frameId);
        JsonObject created = cdp.sendCommand("CSS.createStyleSheet", createParams);
        if (!created.has("styleSheetId")) {
            throw new IOException("Chrome did not return a stylesheet id");
        }

        JsonObject textParams = new JsonObject();
        textParams.addProperty("styleSheetId", created.get("styleSheetId").getAsString());
        textParams.addProperty("text", stylesheet);
        cdp.sendCommand("CSS.setStyleSheetText", textParams);
    }

    static void validateLandscape(byte[] pdf) throws IOException {
        try (PDDocument document = Loader.loadPDF(pdf)) {
            for (int i = 0; i < document.getNumberOfPages(); i++) {
                PDPage page = document.getPage(i);
                PDRectangle mediaBox = page.getMediaBox();
                float width = mediaBox.getWidth();
                float height = mediaBox.getHeight();
                int rotation = page.getRotation() % 360;
                if (rotation == 90 || rotation == 270) {
                    float tmp = width;
                    width = height;
                    height = tmp;
                }
                if (width <= height) {
                    throw new IOException("Landscape was requested, but Chrome rendered page " + (i + 1) + " in portrait (" + width + " x " + height + " pt). The email's print CSS can set page orientation.");
                }
            }
        }
    }

    private ChromeBrowserProvisioner.Browser resolveBrowser() throws Exception {
        ChromeBrowserProvisioner.Browser browser = resolvedBrowser;
        if (browser == null) {
            synchronized (this) {
                browser = resolvedBrowser;
                if (browser == null) {
                    browser = new ChromeBrowserProvisioner(options).resolve();
                    resolvedBrowser = browser;
                    Logger.debug("Use %s", browser.describe());
                }
            }
        }
        return browser;
    }

    private void logUnsupportedOptions() {
        if (options.getDpi() != null && !options.getDpi().isEmpty() && !"300".equals(options.getDpi())) {
            Logger.error("Chrome renderer ignores the DPI setting: %s", options.getDpi());
        }
        if (!options.getExtParams().isEmpty()) {
            Logger.error("Chrome renderer ignores legacy wkhtmltopdf parameters: %s", String.join(" ", options.getExtParams()));
        }
    }

    static String resolveExecutable(String configuredExecutable) {
        String executable = findInstalledExecutable(configuredExecutable);
        if (executable == null) {
            throw new IllegalStateException("Chrome/Chromium/Edge executable not found. Install one or set CHROME_PATH.");
        }
        return executable;
    }

    static String findInstalledExecutable(String configuredExecutable) {
        String configured = configuredExecutable;
        if (configured != null && !configured.trim().isEmpty()) {
            return requireExecutable(configured.trim());
        }

        String[] envNames = {"CHROME_PATH", "CHROME_BIN", "GOOGLE_CHROME_BIN", "CHROMIUM_PATH"};
        for (String envName : envNames) {
            String value = System.getenv(envName);
            if (value != null && !value.trim().isEmpty()) {
                return requireExecutable(value.trim());
            }
        }

        List<String> candidates = new ArrayList<>(Arrays.asList(
                "google-chrome", "google-chrome-stable", "chromium", "chromium-browser", "microsoft-edge", "microsoft-edge-stable", "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome", "/Applications/Chromium.app/Contents/MacOS/Chromium", "/Applications/Microsoft Edge.app/Contents/MacOS/Microsoft Edge"));
        addWindowsCandidates(candidates);

        for (String candidate : candidates) {
            String executable = findExecutable(candidate);
            if (executable != null) {
                return executable;
            }
        }

        return null;
    }

    private static void addWindowsCandidates(List<String> candidates) {
        addCandidate(candidates, "ProgramFiles", "Google\\Chrome\\Application\\chrome.exe");
        addCandidate(candidates, "ProgramFiles(x86)", "Google\\Chrome\\Application\\chrome.exe");
        addCandidate(candidates, "LOCALAPPDATA", "Google\\Chrome\\Application\\chrome.exe");
        addCandidate(candidates, "ProgramFiles", "Microsoft\\Edge\\Application\\msedge.exe");
        addCandidate(candidates, "ProgramFiles(x86)", "Microsoft\\Edge\\Application\\msedge.exe");
    }

    private static void addCandidate(List<String> candidates, String envName, String relativePath) {
        String base = System.getenv(envName);
        if (base != null && !base.isEmpty()) {
            candidates.add(base + File.separator + relativePath);
        }
    }

    private static String findExecutable(String candidate) {
        File file = new File(candidate);
        if (file.isFile()) {
            return file.getAbsolutePath();
        }

        String path = System.getenv("PATH");
        if (path == null || path.isEmpty()) {
            return null;
        }
        for (String directory : path.split(File.pathSeparator)) {
            File executable = new File(directory, candidate);
            if (executable.isFile()) {
                return executable.getAbsolutePath();
            }
        }
        return null;
    }

    private static String requireExecutable(String executable) {
        String found = findExecutable(executable);
        if (found == null) {
            throw new IllegalStateException("Chrome/Chromium executable not found: " + executable);
        }
        return found;
    }

    private static boolean noSandbox() {
        String value = System.getenv("CHROME_NO_SANDBOX");
        return value != null && ("1".equals(value) || "true".equalsIgnoreCase(value));
    }

    private static int freePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    private static String waitForPageWebSocket(int port, String executable, Process browser, File browserLog) throws Exception {
        long deadline = System.currentTimeMillis() + START_TIMEOUT_MS;
        Exception lastError = null;
        while (System.currentTimeMillis() < deadline) {
            if (!browser.isAlive()) {
                throw new IOException(withBrowserLog(
                        "Chrome exited before DevTools was ready (executable=" + executable + ", log=" + browserLog + ")", browserLog));
            }
            try {
                URL url = new URL("http://127.0.0.1:" + port + "/json/list");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection(Proxy.NO_PROXY);
                connection.setConnectTimeout(500);
                connection.setReadTimeout(1000);
                try (InputStream input = connection.getInputStream()) {
                    JsonArray targets = JsonParser.parseString(readString(input)).getAsJsonArray();
                    for (JsonElement target : targets) {
                        JsonObject object = target.getAsJsonObject();
                        if (object.has("type") && "page".equals(object.get("type").getAsString()) && object.has("webSocketDebuggerUrl")) {
                            return object.get("webSocketDebuggerUrl").getAsString();
                        }
                    }
                }
            } catch (Exception e) {
                lastError = e;
            }
            Thread.sleep(100);
        }
        throw new IOException(withBrowserLog("Timed out waiting for Chrome DevTools page target", browserLog), lastError);
    }

    private static String withBrowserLog(String message, File browserLog) {
        String logTail = readLogTail(browserLog, 4096);
        return logTail.isEmpty() ? message : message + "\n" + logTail;
    }

    private static String readLogTail(File browserLog, int maxBytes) {
        try {
            byte[] bytes = Files.readAllBytes(browserLog.toPath());
            int offset = Math.max(0, bytes.length - maxBytes);
            return new String(bytes, offset, bytes.length - offset, StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "";
        }
    }

    private static String readString(InputStream input) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int read;
        while ((read = input.read(buffer)) != -1) {
            output.write(buffer, 0, read);
        }
        return new String(output.toByteArray(), StandardCharsets.UTF_8);
    }

    private static void deleteRecursively(Path directory) {
        if (directory == null || !Files.exists(directory)) {
            return;
        }
        try (Stream<Path> paths = Files.walk(directory)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException ignored) {
                    // best effort temp cleanup
                }
            });
        } catch (IOException | RuntimeException ignored) {
            // best effort temp cleanup; Chrome may still be releasing profile files
        }
    }

    private static void deleteTempFile(File file) {
        if (file != null && !file.delete()) {
            file.deleteOnExit();
        }
    }
}
