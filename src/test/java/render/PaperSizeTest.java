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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class PaperSizeTest {
    @Test
    public void resolvesAllSupportedSizesCaseInsensitively() {
        assertEquals(30, PaperSize.all().size());
        for (PaperSize paperSize : PaperSize.all()) {
            assertEquals(paperSize.getName(), PaperSize.fromName(paperSize.getName().toLowerCase()).getName());
            assertTrue(paperSize.getWidthPoints() > 0);
            assertTrue(paperSize.getHeightPoints() > 0);
        }
    }

    @Test
    public void resolvesCommonSizesInPoints() {
        assertEquals(595.2756, PaperSize.fromName("A4").getWidthPoints(), 0.001);
        assertEquals(841.8898, PaperSize.fromName("A4").getHeightPoints(), 0.001);
        assertEquals(612.0, PaperSize.fromName("Letter").getWidthPoints(), 0.001);
        assertEquals(792.0, PaperSize.fromName("Letter").getHeightPoints(), 0.001);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsUnknownSize() {
        PaperSize.fromName("nope");
    }
}
