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

package cli;

import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import org.junit.Test;

public class CliBrowserDownloadProgressTest {
    @Test
    public void logsEveryTwentyPercentWhenNotInteractive() throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        CliBrowserDownloadProgress progress = new CliBrowserDownloadProgress(new PrintStream(bytes, true, "UTF-8"), false);

        for (int percent = 0; percent <= 100; percent++) {
            progress.progress("Downloading Chrome headless shell 157.0.8103.38", percent);
        }
        progress.finished();

        String output = bytes.toString("UTF-8");
        for (int percent = 0; percent <= 100; percent += 20) {
            assertTrue(output.contains(" " + percent + "%"));
        }
        assertTrue(output.contains("Chrome headless shell ready."));
    }

    @Test
    public void redrawsAnInteractiveProgressBar() throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        CliBrowserDownloadProgress progress = new CliBrowserDownloadProgress(new PrintStream(bytes, true, "UTF-8"), true);

        progress.progress("Downloading Chrome headless shell 157.0.8103.38", 50);

        String output = bytes.toString("UTF-8");
        assertTrue(output.startsWith("\r"));
        assertTrue(output.contains("50%"));
        assertTrue(output.contains("############------------"));
    }
}
