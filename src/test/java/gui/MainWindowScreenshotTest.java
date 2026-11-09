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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Window;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.PrintStream;
import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import org.junit.Assume;
import org.junit.Test;

public class MainWindowScreenshotTest {
    private static final long VISIBLE_TIMEOUT_MS = 5000;

    @Test
    public void capturesMainWindow() throws Exception {
        Assume.assumeTrue("GUI_SCREENSHOT is not set", "1".equals(System.getenv("GUI_SCREENSHOT")));
        Assume.assumeFalse("A display is required", GraphicsEnvironment.isHeadless());

        PrintStream originalOut = System.out;
        PrintStream originalErr = System.err;
        final JFrame[] frame = new JFrame[1];
        final String outputPath = screenshotPath();

        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    new MainWindow();
                    frame[0] = findMainFrame();
                    if (frame[0] != null) {
                        frame[0].setVisible(true);
                    }
                }
            });
            assertNotNull("Main window was not created", frame[0]);
            waitUntilVisible(frame[0]);

            Thread.sleep(500);
            Rectangle bounds = frame[0].getBounds();
            BufferedImage screenshot = new Robot().createScreenCapture(bounds);

            File output = new File(outputPath);
            File parent = output.getParentFile();
            if (parent != null) {
                assertTrue("Could not create screenshot directory", parent.isDirectory() || parent.mkdirs());
            }
            assertTrue("Could not write screenshot", ImageIO.write(screenshot, "png", output));
            assertTrue("Screenshot is empty", output.isFile() && output.length() > 0);

            BufferedImage written = ImageIO.read(output);
            assertNotNull("Screenshot is not a valid image", written);
            assertTrue("Screenshot has invalid dimensions", written.getWidth() > 0 && written.getHeight() > 0);
        } finally {
            if (frame[0] != null) {
                SwingUtilities.invokeAndWait(new Runnable() {
                    @Override
                    public void run() {
                        frame[0].dispose();
                    }
                });
            }
            System.setOut(originalOut);
            System.setErr(originalErr);
        }
    }

    private static String screenshotPath() {
        String path = System.getenv("GUI_SCREENSHOT_PATH");
        return path == null || path.trim().isEmpty() ? "build/screenshots/main-window.png" : path;
    }

    private static void waitUntilVisible(final JFrame frame) throws Exception {
        long deadline = System.currentTimeMillis() + VISIBLE_TIMEOUT_MS;
        while (System.currentTimeMillis() < deadline) {
            final boolean[] visible = new boolean[1];
            SwingUtilities.invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    visible[0] = frame.isShowing() && frame.isVisible();
                }
            });
            if (visible[0]) {
                return;
            }
            Thread.sleep(50);
        }
        fail("Main window did not become visible within " + VISIBLE_TIMEOUT_MS + "ms");
    }

    private static JFrame findMainFrame() {
        for (Window window : Window.getWindows()) {
            if (window instanceof JFrame && "Email to PDF Converter".equals(((JFrame) window).getTitle())) {
                return (JFrame) window;
            }
        }
        return null;
    }
}
