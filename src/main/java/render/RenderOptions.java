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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Renderer-facing settings.
 */
public final class RenderOptions {
    public enum Renderer {
        WKHTMLTOPDF, CHROME;

        public static Renderer fromName(String name) {
            if (name == null || name.trim().isEmpty() || "chrome".equalsIgnoreCase(name.trim()) || "chromium".equalsIgnoreCase(name.trim())) {
                return CHROME;
            }
            if ("wkhtmltopdf".equalsIgnoreCase(name.trim())) {
                return WKHTMLTOPDF;
            }
            throw new IllegalArgumentException("Unsupported renderer: " + name);
        }
    }

    public enum BrowserDownload {
        AUTO, NEVER, ONCE, LATEST, FORCE;

        public static BrowserDownload fromName(String name) {
            if (name == null || name.trim().isEmpty() || "auto".equalsIgnoreCase(name.trim())) {
                return AUTO;
            }
            if ("never".equalsIgnoreCase(name.trim())) {
                return NEVER;
            }
            if ("once".equalsIgnoreCase(name.trim())) {
                return ONCE;
            }
            if ("latest".equalsIgnoreCase(name.trim())) {
                return LATEST;
            }
            if ("force".equalsIgnoreCase(name.trim())) {
                return FORCE;
            }
            throw new IllegalArgumentException("Unsupported browser download mode: " + name);
        }
    }

    public enum Orientation {
        PORTRAIT, LANDSCAPE;

        public static Orientation fromName(String name) {
            if (name == null || name.trim().isEmpty() || "portrait".equalsIgnoreCase(name.trim())) {
                return PORTRAIT;
            }
            if ("landscape".equalsIgnoreCase(name.trim())) {
                return LANDSCAPE;
            }
            throw new IllegalArgumentException("Unsupported page orientation: " + name);
        }

        public String getWkhtmltopdfName() {
            return this == LANDSCAPE ? "Landscape" : "Portrait";
        }
    }

    private final Renderer renderer;
    private final String executable;
    private final String proxy;
    private final String pageSize;
    private final String dpi;
    private final boolean javaScriptEnabled;
    private final List<String> extParams;
    private final BrowserDownload browserDownload;
    private final String browserCacheDir;
    private final Orientation orientation;
    private final String printStylesheet;

    public RenderOptions(
                         Renderer renderer, String executable, String proxy, String pageSize, String dpi, boolean javaScriptEnabled, List<String> extParams) {
        this(renderer, executable, proxy, pageSize, dpi, javaScriptEnabled, extParams, BrowserDownload.AUTO, null, Orientation.PORTRAIT);
    }

    public RenderOptions(
                         Renderer renderer, String executable, String proxy, String pageSize, String dpi, boolean javaScriptEnabled, List<String> extParams, BrowserDownload browserDownload, String browserCacheDir) {
        this(renderer, executable, proxy, pageSize, dpi, javaScriptEnabled, extParams, browserDownload, browserCacheDir, Orientation.PORTRAIT);
    }

    public RenderOptions(
                         Renderer renderer, String executable, String proxy, String pageSize, String dpi, boolean javaScriptEnabled, List<String> extParams, BrowserDownload browserDownload, String browserCacheDir, Orientation orientation) {
        this(renderer, executable, proxy, pageSize, dpi, javaScriptEnabled, extParams, browserDownload, browserCacheDir, orientation, null);
    }

    public RenderOptions(
                         Renderer renderer, String executable, String proxy, String pageSize, String dpi, boolean javaScriptEnabled, List<String> extParams, BrowserDownload browserDownload, String browserCacheDir, Orientation orientation, String printStylesheet) {
        this.renderer = renderer;
        this.executable = executable;
        this.proxy = proxy;
        this.pageSize = pageSize;
        this.dpi = dpi;
        this.javaScriptEnabled = javaScriptEnabled;
        this.extParams = extParams == null ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(extParams));
        this.browserDownload = browserDownload == null ? BrowserDownload.AUTO : browserDownload;
        this.browserCacheDir = browserCacheDir;
        this.orientation = orientation == null ? Orientation.PORTRAIT : orientation;
        this.printStylesheet = printStylesheet;
    }

    /**
     * Options for the legacy wkhtmltopdf-only API.
     */
    public static RenderOptions wkhtmltopdf(List<String> extParams) {
        return new RenderOptions(Renderer.WKHTMLTOPDF, null, null, null, null, false, extParams);
    }

    public Renderer getRenderer() {
        return renderer;
    }

    public String getExecutable() {
        return executable;
    }

    public String getProxy() {
        return proxy;
    }

    public String getPageSize() {
        return pageSize;
    }

    public String getDpi() {
        return dpi;
    }

    public boolean isJavaScriptEnabled() {
        return javaScriptEnabled;
    }

    public List<String> getExtParams() {
        return extParams;
    }

    public BrowserDownload getBrowserDownload() {
        return browserDownload;
    }

    public String getBrowserCacheDir() {
        return browserCacheDir;
    }

    public Orientation getOrientation() {
        return orientation;
    }

    public String getPrintStylesheet() {
        return printStylesheet;
    }
}
