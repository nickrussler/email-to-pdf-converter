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

package mimeparser;

import java.awt.geom.Point2D;
import java.io.File;
import java.io.IOException;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.contentstream.PDFGraphicsStreamEngine;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.graphics.image.PDImage;

/**
 * Measures the vertical extent of painted content in a PDF.
 *
 * <p>This is used to determine the height of the generated header without rendering the PDF to an image or executing
 * JavaScript.</p>
 */
public final class PdfContentHeightMeasurer {
    private static final float FULL_PAGE_RATIO = 0.99f;

    private PdfContentHeightMeasurer() {
    }

    /**
     * Measure the height of painted content in CSS pixels.
     *
     * @param pdfFile       PDF to inspect
     * @param targetWidthPx width of the HTML viewport used to create the PDF
     * @return content height in CSS pixels, or 0 if no painted content was found
     * @throws IOException if the PDF could not be read
     */
    public static int measureContentHeightPx(File pdfFile, int targetWidthPx) throws IOException {
        try (PDDocument document = Loader.loadPDF(pdfFile)) {
            if (document.getNumberOfPages() == 0) {
                return 0;
            }

            float pageWidthPt = document.getPage(0).getMediaBox().getWidth();
            if (pageWidthPt <= 0) {
                return 0;
            }

            float totalHeightPt = 0;
            for (PDPage page : document.getPages()) {
                ContentBounds bounds = new ContentBounds(page);
                bounds.processPage(page);
                totalHeightPt += bounds.getContentHeightPt();
            }

            if (totalHeightPt <= 0) {
                return 0;
            }

            return Math.round(totalHeightPt * targetWidthPx / pageWidthPt);
        }
    }

    private static final class ContentBounds extends PDFGraphicsStreamEngine {
        private final float pageWidth;
        private final float pageHeight;

        private boolean pathActive;
        private float pathMinX;
        private float pathMinY;
        private float pathMaxX;
        private float pathMaxY;
        private float currentX;
        private float currentY;

        private float contentMinY = Float.POSITIVE_INFINITY;
        private float contentMaxY = Float.NEGATIVE_INFINITY;

        ContentBounds(PDPage page) {
            super(page);
            pageWidth = page.getMediaBox().getWidth();
            pageHeight = page.getMediaBox().getHeight();
        }

        float getContentHeightPt() {
            if (contentMinY == Float.POSITIVE_INFINITY || contentMaxY == Float.NEGATIVE_INFINITY) {
                return 0;
            }
            return contentMaxY - contentMinY;
        }

        private void beginPathIfNeeded() {
            if (!pathActive) {
                pathActive = true;
                pathMinX = Float.POSITIVE_INFINITY;
                pathMinY = Float.POSITIVE_INFINITY;
                pathMaxX = Float.NEGATIVE_INFINITY;
                pathMaxY = Float.NEGATIVE_INFINITY;
            }
        }

        private void addPoint(float x, float y) {
            beginPathIfNeeded();
            if (x < pathMinX) {
                pathMinX = x;
            }
            if (x > pathMaxX) {
                pathMaxX = x;
            }
            if (y < pathMinY) {
                pathMinY = y;
            }
            if (y > pathMaxY) {
                pathMaxY = y;
            }
            currentX = x;
            currentY = y;
        }

        private void finishPath(boolean painted) {
            if (pathActive && painted && !isFullPage()) {
                if (pathMinY < contentMinY) {
                    contentMinY = pathMinY;
                }
                if (pathMaxY > contentMaxY) {
                    contentMaxY = pathMaxY;
                }
            }
            pathActive = false;
        }

        private boolean isFullPage() {
            float width = pathMaxX - pathMinX;
            float height = pathMaxY - pathMinY;
            return width >= pageWidth * FULL_PAGE_RATIO && height >= pageHeight * FULL_PAGE_RATIO;
        }

        @Override
        public void appendRectangle(Point2D p0, Point2D p1, Point2D p2, Point2D p3) {
            addPoint((float) p0.getX(), (float) p0.getY());
            addPoint((float) p1.getX(), (float) p1.getY());
            addPoint((float) p2.getX(), (float) p2.getY());
            addPoint((float) p3.getX(), (float) p3.getY());
        }

        @Override
        public void drawImage(PDImage pdImage) {
            // Generated headers only contain text and vector table styling.
        }

        @Override
        public void clip(int windingRule) {
        }

        @Override
        public void moveTo(float x, float y) {
            addPoint(x, y);
        }

        @Override
        public void lineTo(float x, float y) {
            addPoint(x, y);
        }

        @Override
        public void curveTo(float x1, float y1, float x2, float y2, float x3, float y3) {
            addPoint(x1, y1);
            addPoint(x2, y2);
            addPoint(x3, y3);
        }

        @Override
        public Point2D getCurrentPoint() {
            return new Point2D.Float(currentX, currentY);
        }

        @Override
        public void closePath() {
        }

        @Override
        public void endPath() {
            finishPath(false);
        }

        @Override
        public void strokePath() {
            finishPath(true);
        }

        @Override
        public void fillPath(int windingRule) {
            finishPath(true);
        }

        @Override
        public void fillAndStrokePath(int windingRule) {
            finishPath(true);
        }

        @Override
        public void shadingFill(COSName shadingName) {
            finishPath(true);
        }
    }
}
