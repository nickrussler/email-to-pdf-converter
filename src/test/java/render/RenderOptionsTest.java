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

import org.junit.Test;

public class RenderOptionsTest {
    @Test
    public void orientationDefaultsToPortrait() {
        RenderOptions options = new RenderOptions(RenderOptions.Renderer.CHROME, null, null, "A4", "300", false, null);
        assertEquals(RenderOptions.Orientation.PORTRAIT, options.getOrientation());
    }

    @Test
    public void parsesOrientationCaseInsensitively() {
        assertEquals(RenderOptions.Orientation.PORTRAIT, RenderOptions.Orientation.fromName(null));
        assertEquals(RenderOptions.Orientation.PORTRAIT, RenderOptions.Orientation.fromName(""));
        assertEquals(RenderOptions.Orientation.PORTRAIT, RenderOptions.Orientation.fromName("Portrait"));
        assertEquals(RenderOptions.Orientation.LANDSCAPE, RenderOptions.Orientation.fromName("landscape"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsUnknownOrientation() {
        RenderOptions.Orientation.fromName("diagonal");
    }
}
