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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ChromeRendererCssTest {
    @Test
    public void removesSizeFromPageRulesButKeepsOtherProperties() {
        String html = "<style>@page WordSection1 {size:8.5in 11.0in; margin:1.0in;} @page :first { size: A4 landscape; }</style>";

        String stripped = ChromeRenderer.stripCssPageSize(html);

        assertTrue(stripped, stripped.contains("@page WordSection1"));
        assertTrue(stripped, stripped.contains("margin:1.0in"));
        assertFalse(stripped, stripped.toLowerCase().contains("size:"));
    }

    @Test
    public void leavesHtmlWithoutPageRulesUntouched() {
        String html = "<style>.body { width: 10px; }</style><p>text</p>";
        assertEquals(html, ChromeRenderer.stripCssPageSize(html));
    }
}
