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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URI;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import render.RenderOptions;
import util.Logger;

/**
 * Finds or downloads the browser used by {@link ChromeRenderer}.
 */
final class ChromeBrowserProvisioner {
    static final String METADATA_URL = "https://googlechromelabs.github.io/chrome-for-testing/last-known-good-versions-with-downloads.json";
    static final String CACHE_DIR_NAME = "chrome-headless-shell";
    static final String COMPLETE_MARKER = "INSTALLATION_COMPLETE";

    private static final int CONNECT_TIMEOUT_MS = 30000;
    private static final int READ_TIMEOUT_MS = 30000;
    private static final int DOWNLOAD_ATTEMPTS = 3;
    private static final long LOCK_TIMEOUT_MS = 120000;

    private final RenderOptions options;
    private final String metadataUrl;
    private final Path cacheRoot;
    private Platform platform;

    ChromeBrowserProvisioner(RenderOptions options) {
        this(options, METADATA_URL, defaultCacheRoot(options.getBrowserCacheDir()), null);
    }

    ChromeBrowserProvisioner(RenderOptions options, String metadataUrl, Path cacheRoot, Platform platform) {
        this.options = options;
        this.metadataUrl = metadataUrl;
        this.cacheRoot = cacheRoot.toAbsolutePath().normalize();
        this.platform = platform;
    }

    Browser resolve() throws Exception {
        String explicit = options.getExecutable();
        if (explicit != null && !explicit.trim().isEmpty()) {
            return Browser.installed(ChromeRenderer.resolveExecutable(explicit.trim()), "explicit path");
        }

        RenderOptions.BrowserDownload mode = options.getBrowserDownload();
        if (mode == RenderOptions.BrowserDownload.AUTO || mode == RenderOptions.BrowserDownload.NEVER) {
            String installed = ChromeRenderer.findInstalledExecutable(null);
            if (installed != null) {
                return Browser.installed(installed, "installed browser");
            }

            Browser cached = findCachedBrowser(null);
            if (cached != null) {
                return cached;
            }

            if (mode == RenderOptions.BrowserDownload.NEVER) {
                throw new IllegalStateException(
                        "Chrome/Chromium/Edge executable not found and browser downloads are disabled (--browser-download never)." + " Install Chrome/Chromium/Edge or use --browser-download auto.");
            }

            return provisionLatest();
        }

        if (mode == RenderOptions.BrowserDownload.ONCE) {
            Browser cached = findCachedBrowser(null);
            return cached != null ? cached : provisionLatest();
        }

        if (mode == RenderOptions.BrowserDownload.LATEST) {
            Metadata metadata = fetchMetadata();
            Browser cached = findCachedBrowser(metadata.version);
            return cached != null ? cached : provision(metadata);
        }

        throw new IllegalArgumentException("Unsupported browser download mode: " + mode);
    }

    private Browser provisionLatest() throws Exception {
        try (CacheLock ignored = lock()) {
            Browser cached = findCachedBrowser(null);
            if (cached != null) {
                return cached;
            }
            return download(fetchMetadata());
        }
    }

    private Browser provision(Metadata metadata) throws Exception {
        try (CacheLock ignored = lock()) {
            Browser cached = findCachedBrowser(metadata.version);
            return cached != null ? cached : download(metadata);
        }
    }

    private Browser download(Metadata metadata) throws Exception {
        Platform platform = platform();
        checkMusl();
        Logger.info("Downloading Chrome headless shell %s for %s", metadata.version, platform.key);

        Exception lastError = null;
        for (int attempt = 1; attempt <= DOWNLOAD_ATTEMPTS; attempt++) {
            Path archive = null;
            Path staging = null;
            try {
                Files.createDirectories(browserCacheRoot());
                archive = Files.createTempFile(browserCacheRoot(), "chrome-headless-shell-", ".zip");
                downloadFile(metadata.url, archive);

                staging = Files.createTempDirectory(browserCacheRoot(), ".download-");
                extractArchive(archive, staging);
                Path extractedExecutable = staging.resolve(platform.executableRelativePath());
                if (!Files.isRegularFile(extractedExecutable)) {
                    throw new IOException("Downloaded archive does not contain " + platform.executableRelativePath());
                }
                if (!platform.isWindows()) {
                    makeExecutable(staging);
                }
                Files.write(staging.resolve(COMPLETE_MARKER), metadata.version.getBytes(StandardCharsets.UTF_8));

                Path destination = platformDirectory(metadata.version);
                Files.createDirectories(destination.getParent());
                deleteRecursively(destination);
                publish(staging, destination);
                staging = null;
                deleteFile(archive);
                archive = null;

                Path executable = destination.resolve(platform.executableRelativePath());
                if (!Files.isRegularFile(executable)) {
                    throw new IOException("Downloaded Chrome headless shell executable is missing: " + executable);
                }
                return Browser.downloaded(executable.toAbsolutePath().toString(), metadata.version);
            } catch (Exception e) {
                lastError = e;
                deleteRecursively(staging);
                deleteFile(archive);
                if (attempt < DOWNLOAD_ATTEMPTS) {
                    Logger.error("Chrome headless shell download failed (attempt %d/%d): %s", attempt, DOWNLOAD_ATTEMPTS, e.getMessage());
                }
            }
        }

        throw new IOException(
                "Could not download Chrome headless shell " + metadata.version + " from " + metadata.url + ": " + lastError.getMessage(), lastError);
    }

    private Browser findCachedBrowser(String exactVersion) throws IOException {
        Platform platform = platform();
        Path root = browserCacheRoot();
        if (!Files.isDirectory(root)) {
            return null;
        }

        List<Browser> cached = new ArrayList<>();
        try (Stream<Path> versions = Files.list(root)) {
            versions.filter(Files::isDirectory).forEach(versionDirectory -> {
                String version = versionDirectory.getFileName().toString();
                if (exactVersion != null && !exactVersion.equals(version)) {
                    return;
                }
                Path platformDirectory = versionDirectory.resolve(platform.key);
                Path executable = platformDirectory.resolve(platform.executableRelativePath());
                Path marker = platformDirectory.resolve(COMPLETE_MARKER);
                if (Files.isRegularFile(marker) && Files.isRegularFile(executable)) {
                    cached.add(Browser.cached(executable.toAbsolutePath().toString(), version));
                }
            });
        }

        if (cached.isEmpty()) {
            return null;
        }
        Collections.sort(cached, new Comparator<Browser>() {
            @Override
            public int compare(Browser left, Browser right) {
                return compareVersions(right.version, left.version);
            }
        });
        return cached.get(0);
    }

    private Metadata fetchMetadata() throws IOException {
        Platform platform = platform();
        try {
            String body = readUrl(metadataUrl);
            JsonObject root = JsonParser.parseString(body).getAsJsonObject();
            JsonObject stable = root.getAsJsonObject("channels").getAsJsonObject("Stable");
            String version = stable.get("version").getAsString();
            if (!version.matches("[0-9A-Za-z._-]+")) {
                throw new IOException("Chrome for Testing returned an invalid version: " + version);
            }

            JsonArray downloads = stable.getAsJsonObject("downloads").getAsJsonArray("chrome-headless-shell");
            for (JsonElement download : downloads) {
                JsonObject entry = download.getAsJsonObject();
                if (platform.key.equals(entry.get("platform").getAsString())) {
                    String url = entry.get("url").getAsString();
                    if (!url.startsWith("https://") && !url.startsWith("http://")) {
                        throw new IOException("Chrome for Testing returned an unsupported download URL: " + url);
                    }
                    return new Metadata(version, url);
                }
            }

            throw new IOException("Chrome for Testing has no chrome-headless-shell download for platform " + platform.key);
        } catch (RuntimeException e) {
            throw new IOException("Could not parse Chrome for Testing metadata from " + metadataUrl, e);
        }
    }

    private String readUrl(String value) throws IOException {
        HttpURLConnection connection = openConnection(value);
        try {
            int status = connection.getResponseCode();
            if (status != HttpURLConnection.HTTP_OK) {
                throw new IOException("HTTP " + status + " from " + value);
            }
            try (InputStream input = connection.getInputStream()) {
                return new String(readAll(input), StandardCharsets.UTF_8);
            }
        } finally {
            connection.disconnect();
        }
    }

    private void downloadFile(String value, Path target) throws IOException {
        HttpURLConnection connection = openConnection(value);
        try {
            int status = connection.getResponseCode();
            if (status != HttpURLConnection.HTTP_OK) {
                throw new IOException("HTTP " + status + " from " + value);
            }
            long expectedSize = connection.getContentLengthLong();
            try (InputStream input = connection.getInputStream(); OutputStream output = Files.newOutputStream(target)) {
                copy(input, output);
            }
            long actualSize = Files.size(target);
            if (expectedSize >= 0 && actualSize != expectedSize) {
                throw new IOException("Downloaded " + actualSize + " bytes but expected " + expectedSize + " from " + value);
            }
        } finally {
            connection.disconnect();
        }
    }

    private HttpURLConnection openConnection(String value) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(value).openConnection(parseProxy(options.getProxy()));
        connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
        connection.setReadTimeout(READ_TIMEOUT_MS);
        connection.setInstanceFollowRedirects(true);
        connection.setUseCaches(false);
        return connection;
    }

    private void extractArchive(Path archive, Path root) throws IOException {
        Path normalizedRoot = root.toAbsolutePath().normalize();
        try (ZipInputStream input = new ZipInputStream(Files.newInputStream(archive))) {
            ZipEntry entry;
            while ((entry = input.getNextEntry()) != null) {
                Path target = normalizedRoot.resolve(entry.getName()).normalize();
                if (!target.startsWith(normalizedRoot)) {
                    throw new IOException("Archive contains an invalid path: " + entry.getName());
                }
                if (entry.isDirectory()) {
                    Files.createDirectories(target);
                } else {
                    Files.createDirectories(target.getParent());
                    try (OutputStream output = Files.newOutputStream(target)) {
                        copy(input, output);
                    }
                }
                input.closeEntry();
            }
        }
    }

    private static void makeExecutable(Path root) throws IOException {
        try (Stream<Path> paths = Files.walk(root)) {
            paths.filter(Files::isRegularFile).forEach(path -> {
                String name = path.getFileName().toString();
                if ("chrome-headless-shell".equals(name) || "chrome-headless-shell.exe".equals(name) || "chrome_crashpad_handler".equals(name) || "chrome_sandbox".equals(name)) {
                    if (!path.toFile().setExecutable(true, false) && !Files.isExecutable(path)) {
                        throw new IllegalStateException("Could not make Chrome headless shell executable: " + path);
                    }
                }
            });
        }
    }

    private static void publish(Path staging, Path destination) throws IOException {
        try {
            Files.move(staging, destination, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(staging, destination);
        }
    }

    private void checkMusl() throws IOException {
        if (!platform().isLinux()) {
            return;
        }

        String osRelease = readSmallFile(Paths.get("/etc/os-release")).toLowerCase(Locale.ROOT).replace("\"", "");
        if (osRelease.contains("id=alpine")) {
            throw new IOException("Automatic Chrome headless shell downloads are not supported on musl-based Linux. Install Chromium or use --renderer-executable.");
        }

        if (Files.exists(Paths.get("/lib/ld-musl-x86_64.so.1")) || Files.exists(Paths.get("/lib/ld-musl-aarch64.so.1"))) {
            throw new IOException("Automatic Chrome headless shell downloads are not supported on musl-based Linux. Install Chromium or use --renderer-executable.");
        }

        boolean musl;
        try {
            Process process = new ProcessBuilder("ldd", "--version").redirectErrorStream(true).start();
            String output = new String(readAll(process.getInputStream()), StandardCharsets.UTF_8);
            process.waitFor();
            musl = output.toLowerCase(Locale.ROOT).contains("musl");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while checking the Linux C library", e);
        } catch (IOException ignored) {
            // ldd is not available everywhere; the launch error still includes the browser log.
            musl = false;
        }
        if (musl) {
            throw new IOException("Automatic Chrome headless shell downloads are not supported on musl-based Linux. Install Chromium or use --renderer-executable.");
        }
    }

    private static String readSmallFile(Path path) {
        try {
            return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "";
        }
    }

    private Path browserCacheRoot() {
        return cacheRoot.resolve(CACHE_DIR_NAME);
    }

    private Path platformDirectory(String version) {
        return browserCacheRoot().resolve(version).resolve(platform().key);
    }

    private Platform platform() {
        if (platform == null) {
            platform = currentPlatform();
        }
        return platform;
    }

    private static byte[] readAll(InputStream input) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        copy(input, output);
        return output.toByteArray();
    }

    private static void copy(InputStream input, OutputStream output) throws IOException {
        byte[] buffer = new byte[8192];
        int read;
        while ((read = input.read(buffer)) != -1) {
            output.write(buffer, 0, read);
        }
    }

    static Proxy parseProxy(String value) {
        if (value == null || value.trim().isEmpty()) {
            return Proxy.NO_PROXY;
        }
        try {
            String normalized = value.contains("://") ? value.trim() : "http://" + value.trim();
            URI uri = URI.create(normalized);
            if (uri.getHost() == null) {
                throw new IllegalArgumentException("missing host");
            }
            int port = uri.getPort();
            if (port < 0) {
                port = "https".equalsIgnoreCase(uri.getScheme()) ? 443 : 80;
            }
            Proxy.Type type = uri.getScheme() != null && uri.getScheme().toLowerCase(Locale.ROOT).startsWith("socks") ? Proxy.Type.SOCKS : Proxy.Type.HTTP;
            return new Proxy(type, new InetSocketAddress(uri.getHost(), port));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid proxy for Chrome browser download: " + value, e);
        }
    }

    static int compareVersions(String left, String right) {
        String[] leftParts = left.split("[^0-9]+");
        String[] rightParts = right.split("[^0-9]+");
        int length = Math.max(leftParts.length, rightParts.length);
        for (int i = 0; i < length; i++) {
            long leftPart = i < leftParts.length && !leftParts[i].isEmpty() ? Long.parseLong(leftParts[i]) : 0;
            long rightPart = i < rightParts.length && !rightParts[i].isEmpty() ? Long.parseLong(rightParts[i]) : 0;
            if (leftPart != rightPart) {
                return leftPart < rightPart ? -1 : 1;
            }
        }
        return 0;
    }

    static Platform platformFor(String osName, String osArch) {
        String os = osName.toLowerCase(Locale.ROOT);
        String arch = osArch.toLowerCase(Locale.ROOT);
        if (os.contains("win")) {
            if ("x86".equals(arch) || "i386".equals(arch) || "i686".equals(arch)) {
                return Platform.WIN32;
            }
            if ("amd64".equals(arch) || "x86_64".equals(arch)) {
                return Platform.WIN64;
            }
        } else if (os.contains("mac")) {
            if ("aarch64".equals(arch) || "arm64".equals(arch)) {
                return Platform.MAC_ARM64;
            }
            if ("amd64".equals(arch) || "x86_64".equals(arch)) {
                return Platform.MAC_X64;
            }
        } else if (os.contains("linux")) {
            if ("aarch64".equals(arch) || "arm64".equals(arch)) {
                return Platform.LINUX_ARM64;
            }
            if ("amd64".equals(arch) || "x86_64".equals(arch)) {
                return Platform.LINUX64;
            }
        }
        throw new IllegalArgumentException("Unsupported platform for Chrome headless shell: " + osName + "/" + osArch);
    }

    private static Platform currentPlatform() {
        return platformFor(System.getProperty("os.name", ""), System.getProperty("os.arch", ""));
    }

    static Path defaultCacheRoot(String configuredCacheDir) {
        if (configuredCacheDir != null && !configuredCacheDir.trim().isEmpty()) {
            return Paths.get(configuredCacheDir.trim()).toAbsolutePath().normalize();
        }

        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        String home = System.getProperty("user.home", ".");
        if (os.contains("win")) {
            String localAppData = System.getenv("LOCALAPPDATA");
            Path base = localAppData != null && !localAppData.trim().isEmpty() ? Paths.get(localAppData) : Paths.get(home);
            return base.resolve("email-to-pdf-converter");
        }
        if (os.contains("mac")) {
            return Paths.get(home, "Library", "Caches", "email-to-pdf-converter");
        }

        String xdgCache = System.getenv("XDG_CACHE_HOME");
        Path base = xdgCache != null && !xdgCache.trim().isEmpty() ? Paths.get(xdgCache) : Paths.get(home, ".cache");
        return base.resolve("email-to-pdf-converter");
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
                    // best effort cleanup
                }
            });
        } catch (IOException | RuntimeException ignored) {
            // best effort cleanup
        }
    }

    private static void deleteFile(Path file) {
        if (file == null) {
            return;
        }
        try {
            Files.deleteIfExists(file);
        } catch (IOException ignored) {
            // best effort cleanup
        }
    }

    private CacheLock lock() throws IOException {
        Path lockFile = browserCacheRoot().resolve(".download.lock");
        Files.createDirectories(browserCacheRoot());
        FileChannel channel = FileChannel.open(lockFile, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        long deadline = System.currentTimeMillis() + LOCK_TIMEOUT_MS;
        while (true) {
            FileLock lock = null;
            try {
                lock = channel.tryLock();
            } catch (OverlappingFileLockException ignored) {
                // Another thread in this JVM is downloading.
            }
            if (lock != null) {
                return new CacheLock(channel, lock);
            }
            if (System.currentTimeMillis() >= deadline) {
                channel.close();
                throw new IOException("Timed out waiting for another Chrome headless shell download to finish");
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                channel.close();
                throw new IOException("Interrupted while waiting for another Chrome headless shell download", e);
            }
        }
    }

    private static final class CacheLock implements AutoCloseable {
        private final FileChannel channel;
        private final FileLock lock;

        private CacheLock(FileChannel channel, FileLock lock) {
            this.channel = channel;
            this.lock = lock;
        }

        @Override
        public void close() throws IOException {
            try {
                lock.release();
            } finally {
                channel.close();
            }
        }
    }

    private static final class Metadata {
        private final String version;
        private final String url;

        private Metadata(String version, String url) {
            this.version = version;
            this.url = url;
        }
    }

    static final class Browser {
        private final String executable;
        private final String version;
        private final boolean headlessShell;
        private final String source;

        private Browser(String executable, String version, boolean headlessShell, String source) {
            this.executable = executable;
            this.version = version;
            this.headlessShell = headlessShell;
            this.source = source;
        }

        static Browser installed(String executable, String source) {
            return new Browser(executable, null, isHeadlessShellExecutable(executable), source);
        }

        static Browser cached(String executable, String version) {
            return new Browser(executable, version, true, "cached headless shell");
        }

        static Browser downloaded(String executable, String version) {
            return new Browser(executable, version, true, "downloaded headless shell");
        }

        String getExecutable() {
            return executable;
        }

        String getVersion() {
            return version;
        }

        boolean isHeadlessShell() {
            return headlessShell;
        }

        private static boolean isHeadlessShellExecutable(String executable) {
            return new File(executable).getName().startsWith("chrome-headless-shell");
        }

        String describe() {
            return version == null ? source + ": " + executable : source + " " + version + ": " + executable;
        }
    }

    enum Platform {
        LINUX64("linux64", true, false), LINUX_ARM64("linux-arm64", true, false), MAC_X64("mac-x64", false, false), MAC_ARM64("mac-arm64", false, false), WIN32("win32", false, true), WIN64("win64", false, true);

        private final String key;
        private final boolean linux;
        private final boolean windows;

        Platform(String key, boolean linux, boolean windows) {
            this.key = key;
            this.linux = linux;
            this.windows = windows;
        }

        boolean isLinux() {
            return linux;
        }

        boolean isWindows() {
            return windows;
        }

        String executableRelativePath() {
            return "chrome-headless-shell-" + key + "/chrome-headless-shell" + (windows ? ".exe" : "");
        }
    }
}
