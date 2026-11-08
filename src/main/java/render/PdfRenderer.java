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

import java.io.File;
import java.nio.charset.Charset;

/**
 * Renders prepared HTML to a PDF file.
 */
public interface PdfRenderer {
    /**
     * Render the supplied HTML to the output PDF.
     *
     * @param html      complete HTML document
     * @param charset   charset of the HTML document
     * @param outputPdf target PDF file
     */
    void render(String html, Charset charset, File outputPdf) throws Exception;

    /**
     * Measure the rendered height of the generated header.
     *
     * @param headerHtml complete header HTML document
     * @param widthPx    width of the header viewport in CSS pixels
     * @return header height in CSS pixels
     */
    int measureHeaderHeight(String headerHtml, int widthPx) throws Exception;

    /**
     * Create the renderer selected by the supplied options.
     */
    static PdfRenderer create(RenderOptions options) {
        if (options == null || options.getRenderer() == null) {
            throw new IllegalArgumentException("No renderer was selected");
        }

        switch (options.getRenderer()) {
            case WKHTMLTOPDF:
                return new WkhtmltopdfRenderer(options);
            case CHROME:
                throw new UnsupportedOperationException("Chrome renderer is not implemented yet");
            default:
                throw new IllegalArgumentException("Unsupported renderer: " + options.getRenderer());
        }
    }
}
