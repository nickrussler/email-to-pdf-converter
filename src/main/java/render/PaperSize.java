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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Paper sizes supported by wkhtmltopdf/Qt.
 */
public final class PaperSize {
    private static final double POINTS_PER_MM = 72.0 / 25.4;
    private static final Map<String, PaperSize> SIZES;

    static {
        Map<String, PaperSize> sizes = new HashMap<>();
        add(sizes, "A0", 841, 1189);
        add(sizes, "A1", 594, 841);
        add(sizes, "A2", 420, 594);
        add(sizes, "A3", 297, 420);
        add(sizes, "A4", 210, 297);
        add(sizes, "A5", 148, 210);
        add(sizes, "A6", 105, 148);
        add(sizes, "A7", 74, 105);
        add(sizes, "A8", 52, 74);
        add(sizes, "A9", 37, 52);
        add(sizes, "B0", 1000, 1414);
        add(sizes, "B1", 707, 1000);
        add(sizes, "B2", 500, 707);
        add(sizes, "B3", 353, 500);
        add(sizes, "B4", 250, 353);
        add(sizes, "B5", 176, 250);
        add(sizes, "B6", 125, 176);
        add(sizes, "B7", 88, 125);
        add(sizes, "B8", 62, 88);
        add(sizes, "B9", 33, 62);
        add(sizes, "B10", 31, 44);
        add(sizes, "C5E", 163, 229);
        add(sizes, "COMM10E", 105, 241);
        add(sizes, "DLE", 110, 220);
        add(sizes, "EXECUTIVE", 190.5, 254);
        add(sizes, "FOLIO", 210, 330);
        add(sizes, "LEDGER", 431.8, 279.4);
        add(sizes, "LEGAL", 215.9, 355.6);
        add(sizes, "LETTER", 215.9, 279.4);
        add(sizes, "TABLOID", 279.4, 431.8);
        SIZES = Collections.unmodifiableMap(sizes);
    }

    private final String name;
    private final double widthPoints;
    private final double heightPoints;

    private PaperSize(String name, double widthMm, double heightMm) {
        this.name = name;
        this.widthPoints = widthMm * POINTS_PER_MM;
        this.heightPoints = heightMm * POINTS_PER_MM;
    }

    private static void add(Map<String, PaperSize> sizes, String name, double widthMm, double heightMm) {
        sizes.put(name.toUpperCase(Locale.ROOT), new PaperSize(name, widthMm, heightMm));
    }

    public static List<PaperSize> all() {
        return Collections.unmodifiableList(new ArrayList<>(SIZES.values()));
    }

    public static PaperSize fromName(String name) {
        PaperSize size = SIZES.get(name == null ? "" : name.trim().toUpperCase(Locale.ROOT));
        if (size == null) {
            throw new IllegalArgumentException("Unsupported page size: " + name);
        }
        return size;
    }

    public String getName() {
        return name;
    }

    public double getWidthPoints() {
        return widthPoints;
    }

    public double getHeightPoints() {
        return heightPoints;
    }
}
