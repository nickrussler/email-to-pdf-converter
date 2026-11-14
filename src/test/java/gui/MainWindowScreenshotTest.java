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

import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Window;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import javax.imageio.ImageIO;
import javax.swing.DefaultListModel;
import javax.swing.JFrame;
import javax.swing.JList;
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
            String[] demoEmails = prepareDemoEmails();
            MainWindow.main(new String[0]);
            waitUntilFrameCreated(frame);
            assertNotNull("Main window was not created", frame[0]);
            SwingUtilities.invokeAndWait(() -> {
                JList<?> emailList = findEmailList(frame[0]);
                assertNotNull("Email file list was not found", emailList);
                @SuppressWarnings("unchecked") DefaultListModel<String> model = (DefaultListModel<String>) emailList.getModel();
                for (String demoEmail : demoEmails) {
                    model.addElement(demoEmail);
                }
                frame[0].setAlwaysOnTop(true);
                frame[0].toFront();
            });
            waitUntilVisible(frame[0]);

            Robot robot = new Robot(frame[0].getGraphicsConfiguration().getDevice());
            robot.waitForIdle();
            robot.delay(500);
            // The OS draws the title bar, so capture the visible window from the desktop.
            // Do not use the frame bounds directly: on Windows they include an invisible
            // resize border that shows the desktop around the window.
            BufferedImage screenshot = robot.createScreenCapture(visibleWindowBounds(frame[0]));

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

    private static String[] prepareDemoEmails() throws IOException {
        Path fixtures = Paths.get("src", "test", "resources");
        Path demo = Files.createDirectories(Paths.get("build", "Demo Emails"));
        return new String[]{Files.copy(fixtures.resolve("eml/testPlain.eml"), demo.resolve("Order confirmation.eml"), StandardCopyOption.REPLACE_EXISTING).toString(), Files.copy(fixtures.resolve("eml/testAttachments.eml"), demo.resolve("Invoice with attachments.eml"), StandardCopyOption.REPLACE_EXISTING).toString(), Files.copy(fixtures.resolve("msg/testHtml.msg"), demo.resolve("Outlook message.msg"), StandardCopyOption.REPLACE_EXISTING).toString()
        };
    }

    private static JList<?> findEmailList(Container root) {
        for (Component child : root.getComponents()) {
            if (child instanceof JList<?>) {
                return (JList<?>) child;
            }
            if (child instanceof Container) {
                JList<?> list = findEmailList((Container) child);
                if (list != null) {
                    return list;
                }
            }
        }
        return null;
    }

    private static Rectangle visibleWindowBounds(JFrame frame) {
        Rectangle frameBounds = frame.getBounds();
        Point contentLocation = frame.getContentPane().getLocationOnScreen();
        int titleBarHeight = contentLocation.y - frameBounds.y;
        Rectangle bounds = new Rectangle(contentLocation.x, frameBounds.y, frame.getContentPane().getWidth(), titleBarHeight + frame.getContentPane().getHeight());
        assertTrue("Could not determine the visible window bounds", bounds.width > 0 && bounds.height > 0);
        return bounds;
    }

    private static String screenshotPath() {
        String path = System.getenv("GUI_SCREENSHOT_PATH");
        return path == null || path.trim().isEmpty() ? "build/screenshots/main-window.png" : path;
    }

    private static void waitUntilFrameCreated(final JFrame[] frame) throws Exception {
        long deadline = System.currentTimeMillis() + VISIBLE_TIMEOUT_MS;
        while (System.currentTimeMillis() < deadline) {
            SwingUtilities.invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    frame[0] = findMainFrame();
                }
            });
            if (frame[0] != null) {
                return;
            }
            Thread.sleep(50);
        }
        fail("Main window was not created within " + VISIBLE_TIMEOUT_MS + "ms");
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
