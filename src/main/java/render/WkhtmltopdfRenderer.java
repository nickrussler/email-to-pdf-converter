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

import com.google.common.base.Joiner;
import com.google.common.io.Files;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import mimeparser.PdfContentHeightMeasurer;
import util.LogLevel;
import util.Logger;

/**
 * Renders HTML with wkhtmltopdf.
 */
public final class WkhtmltopdfRenderer implements PdfRenderer {
    private static final int HEADER_MIN_HEIGHT = 150;
    private static final int HEADER_PROBE_PAGE_HEIGHT = 5000;
    private static final String VIEWPORT_SIZE = "2480x3508";
    private static final int IMAGE_QUALITY = 100;

    private final RenderOptions options;

    public WkhtmltopdfRenderer(RenderOptions options) {
        this.options = options;
    }

    @Override
    public void render(String html, Charset charset, File outputPdf) throws Exception {
        if (options.getOrientation() == RenderOptions.Orientation.LANDSCAPE) {
            Logger.info("wkhtmltopdf landscape rendering is unreliable for complex HTML tables; use --renderer chrome if the output overlaps.");
        }

        File tmpHtml = File.createTempFile("emailtopdf", ".html");
        try {
            Logger.debug("Write html to temporary file %s", tmpHtml.getAbsolutePath());
            Files.asCharSink(tmpHtml, charset).write(html);

            List<String> cmd = new ArrayList<>(Arrays.asList(
                    executable(), "--viewport-size", VIEWPORT_SIZE, "--image-quality", String.valueOf(IMAGE_QUALITY), "--encoding", charset.name(), "--disable-javascript"));

            addOption(cmd, "--proxy", options.getProxy());
            addOption(cmd, "--page-size", options.getPageSize());
            addOption(cmd, "--orientation", options.getOrientation().getWkhtmltopdfName());
            addOption(cmd, "--dpi", options.getDpi());
            cmd.addAll(options.getExtParams());

            if (options.isJavaScriptEnabled()) {
                cmd.add("--enable-javascript");
            }

            cmd.add(tmpHtml.getAbsolutePath());
            cmd.add(outputPdf.getAbsolutePath());

            Logger.debug("Execute: %s", Joiner.on(' ').join(cmd));
            int exitCode = execCommand(cmd);
            if (!outputPdf.isFile() || outputPdf.length() == 0) {
                throw new IOException(String.format("wkhtmltopdf failed with exit code %d while writing %s", exitCode, outputPdf.getAbsolutePath()));
            }
            if (exitCode != 0) {
                // wkhtmltopdf may report a resource-loading error after producing a usable PDF.
                // Keep the PDF and let conversion continue, including attachment extraction.
                Logger.error("wkhtmltopdf exited with code %d after writing %s; continuing with the generated PDF", exitCode, outputPdf.getAbsolutePath());
            }
        } finally {
            deleteTempFile(tmpHtml);
        }
    }

    @Override
    public int measureHeaderHeight(String headerHtml, int widthPx) {
        File tmpHeader = null;
        File tmpProbe = null;
        try {
            tmpHeader = File.createTempFile("emailtopdf-header", ".html");
            Files.asCharSink(tmpHeader, StandardCharsets.UTF_8).write(headerHtml);

            tmpProbe = File.createTempFile("emailtopdf-header-probe", ".pdf");
            List<String> cmd = new ArrayList<>(Arrays.asList(
                    executable(), "--disable-javascript", "--page-width", widthPx + "px", "--page-height", HEADER_PROBE_PAGE_HEIGHT + "px", "--margin-top", "0", "--margin-bottom", "0", "--margin-left", "0", "--margin-right", "0", "--disable-smart-shrinking", "--quiet", tmpHeader.getAbsolutePath(), tmpProbe.getAbsolutePath()));

            Logger.debug("Execute: %s", Joiner.on(' ').join(cmd));
            int exitCode = execCommand(cmd);
            if (exitCode != 0 || !tmpProbe.isFile() || tmpProbe.length() == 0) {
                Logger.error("Could not measure the header height, using %dpx. Probe exit code: %d", HEADER_MIN_HEIGHT, exitCode);
                return HEADER_MIN_HEIGHT;
            }

            int measuredHeight = PdfContentHeightMeasurer.measureContentHeightPx(tmpProbe, widthPx);
            return Math.max(measuredHeight, HEADER_MIN_HEIGHT);
        } catch (Exception e) {
            Logger.error("Could not measure the header height, using %dpx. Error: %s", HEADER_MIN_HEIGHT, e);
            return HEADER_MIN_HEIGHT;
        } finally {
            deleteTempFile(tmpHeader);
            deleteTempFile(tmpProbe);
        }
    }

    private String executable() {
        if (options.getExecutable() == null || options.getExecutable().isEmpty()) {
            return "wkhtmltopdf";
        }
        return options.getExecutable();
    }

    private static void addOption(List<String> cmd, String name, String value) {
        if (value != null && !value.isEmpty()) {
            cmd.add(name);
            cmd.add(value);
        }
    }

    /**
     * Execute a command and redirect its output to the standard output.
     */
    private static int execCommand(List<String> command) {
        try {
            ProcessBuilder pb = new ProcessBuilder(command);

            if (Logger.level.compareTo(LogLevel.Info) >= 0) {
                pb.inheritIO();
            }

            Process p = pb.start();
            return p.waitFor();
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    private static void deleteTempFile(File file) {
        if (file != null && !file.delete()) {
            file.deleteOnExit();
        }
    }
}
