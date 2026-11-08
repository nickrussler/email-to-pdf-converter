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

package gui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import cli.CommandLineParameters;
import com.beust.jcommander.JCommander;
import java.util.List;
import org.junit.Test;

public class MainWindowArgumentsTest {
    @Test
    public void defaultsToChromeWithAutomaticBrowserDownload() {
        List<String> args = MainWindow.buildConversionArguments("chrome", "", "auto", "", true, null, false, false);
        CommandLineParameters parsed = parse(args);

        assertEquals("chrome", parsed.getRenderer());
        assertEquals("auto", parsed.getBrowserDownload());
        assertNull(parsed.getRendererExecutable());
        assertNull(parsed.getBrowserCacheDir());
        assertFalse(parsed.isDangerousEnableJavaScript());
    }

    @Test
    public void passesEveryBrowserDownloadMode() {
        String[][] modes = {{"auto", "auto"}, {"never", "never"}, {"once", "once"}, {"latest", "latest"}, {"force", "force"}};

        for (String[] mode : modes) {
            CommandLineParameters parsed = parse(MainWindow.buildConversionArguments("chrome", "", mode[0], "", true, null, false, false));
            assertEquals(mode[1], parsed.getBrowserDownload());
        }
    }

    @Test
    public void executableOverrideSuppressesBrowserAcquisitionArguments() {
        List<String> args = MainWindow.buildConversionArguments("chrome", "/opt/Chrome Browser/chrome", "force", "/tmp/browser cache", true, null, false, false);
        CommandLineParameters parsed = parse(args);

        assertEquals("/opt/Chrome Browser/chrome", parsed.getRendererExecutable());
        assertFalse(args.contains("--browser-download"));
        assertFalse(args.contains("--browser-cache-dir"));
        assertNull(parsed.getBrowserCacheDir());
    }

    @Test
    public void wkhtmltopdfOmitsChromeBrowserArguments() {
        List<String> args = MainWindow.buildConversionArguments("wkhtmltopdf", "", "force", "/tmp/browser cache", true, null, false, false);
        CommandLineParameters parsed = parse(args);

        assertEquals("wkhtmltopdf", parsed.getRenderer());
        assertFalse(args.contains("--browser-download"));
        assertFalse(args.contains("--browser-cache-dir"));
        assertFalse(args.contains("--dangerous-enable-javascript"));
        assertNull(parsed.getBrowserCacheDir());
    }

    @Test
    public void pathsWithSpacesAndNonAsciiCharactersRoundTrip() {
        String executable = "/tmp/Chromium Browser/ブラウザ";
        CommandLineParameters parsedExecutable = parse(MainWindow.buildConversionArguments("chrome", executable, "auto", "", true, null, false, false));
        assertEquals(executable, parsedExecutable.getRendererExecutable());

        String cache = "/tmp/browser cache/キャッシュ";
        CommandLineParameters parsedCache = parse(MainWindow.buildConversionArguments("chrome", "", "auto", cache, true, null, false, false));
        assertEquals(cache, parsedCache.getBrowserCacheDir());
    }

    @Test
    public void keepsExistingOptions() {
        List<String> args = MainWindow.buildConversionArguments("chrome", "", "auto", "", false, "http://proxy.test:8080", true, true);
        CommandLineParameters parsed = parse(args);

        assertTrue(parsed.isHideHeaders());
        assertTrue(args.contains("--add-attachment-names"));
        assertTrue(parsed.isExtractAttachments());
        assertEquals("http://proxy.test:8080", parsed.getProxy());
    }

    private CommandLineParameters parse(List<String> args) {
        CommandLineParameters parameters = new CommandLineParameters();
        new JCommander(parameters).parse(args.toArray(new String[args.size()]));
        return parameters;
    }
}
