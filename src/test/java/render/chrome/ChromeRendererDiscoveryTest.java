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
import static org.junit.Assert.assertTrue;

import com.google.common.io.Files;
import java.io.File;
import java.nio.charset.StandardCharsets;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.rules.TemporaryFolder;

public class ChromeRendererDiscoveryTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Rule
    public EnvironmentVariables environment = new EnvironmentVariables();

    @Test
    public void discoversLinuxEdgeExecutableNames() throws Exception {
        environment.clear("CHROME_PATH", "CHROME_BIN", "GOOGLE_CHROME_BIN", "CHROMIUM_PATH");

        for (String executableName : new String[]{"microsoft-edge", "microsoft-edge-stable"}) {
            File directory = temporaryFolder.newFolder(executableName);
            File executable = new File(directory, executableName);
            Files.asCharSink(executable, StandardCharsets.UTF_8).write("#!/bin/sh\nexit 0\n");
            assertTrue(executable.setExecutable(true));

            environment.set("PATH", directory.getAbsolutePath());
            assertEquals(executable.getAbsolutePath(), ChromeRenderer.resolveExecutable(null));
        }
    }
}
