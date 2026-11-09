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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.PrintStream;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import org.junit.Assume;
import org.junit.Test;

public class MainWindowSmokeTest {
    @Test
    public void rendersTheRendererAndBrowserControls() throws Exception {
        Assume.assumeFalse("Desktop smoke test requires a display", GraphicsEnvironment.isHeadless());

        PrintStream originalOut = System.out;
        PrintStream originalErr = System.err;
        final JFrame[] frame = new JFrame[1];
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                new MainWindow();
                frame[0] = findMainFrame();
            }
        });

        try {
            assertNotNull(frame[0]);
            JComboBox<?> renderer = comboWithItem(frame[0].getContentPane(), "wkhtmltopdf");
            assertNotNull(renderer);
            assertEquals("Chrome", renderer.getSelectedItem());
            assertTrue(hasLabel(frame[0].getContentPane(), "Renderer:"));
            assertTrue(hasLabel(frame[0].getContentPane(), "Orientation:"));
            assertTrue(hasLabel(frame[0].getContentPane(), "Executable:"));
            assertTrue(hasLabel(frame[0].getContentPane(), "Download mode:"));
            assertTrue(hasLabel(frame[0].getContentPane(), "Cache directory:"));
            assertTrue(hasLabel(frame[0].getContentPane(), "Custom stylesheet:"));

            JComboBox<?> orientation = comboWithItem(frame[0].getContentPane(), "Landscape");
            assertNotNull(orientation);
            assertEquals("Portrait", orientation.getSelectedItem());

            final JComboBox<?> browserDownload = comboWithItem(frame[0].getContentPane(), "Force");
            assertNotNull(browserDownload);
            assertEquals("Auto", browserDownload.getSelectedItem());
            assertEquals(5, browserDownload.getItemCount());
            final JTextField executable = (JTextField) componentByName(frame[0].getContentPane(), "executableField");
            final JTextField cache = (JTextField) componentByName(frame[0].getContentPane(), "browserCacheField");
            final JTextField customStylesheet = (JTextField) componentByName(frame[0].getContentPane(), "customStylesheetField");
            assertNotNull(executable);
            assertNotNull(cache);
            assertNotNull(customStylesheet);
            assertTrue(browserDownload.isEnabled());
            assertTrue(cache.isEnabled());
            assertTrue(customStylesheet.isEnabled());

            SwingUtilities.invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    browserDownload.setSelectedItem("Force");
                    cache.setText("/tmp/browser cache");
                    customStylesheet.setText("/tmp/custom print.css");
                    executable.setText("/tmp/Chrome");
                }
            });
            assertFalse(browserDownload.isEnabled());
            assertFalse(cache.isEnabled());
            assertTrue(customStylesheet.isEnabled());
            assertEquals("Force", browserDownload.getSelectedItem());
            assertEquals("/tmp/browser cache", cache.getText());
            assertEquals("/tmp/custom print.css", customStylesheet.getText());

            SwingUtilities.invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    executable.setText("");
                }
            });
            assertTrue(browserDownload.isEnabled());
            assertTrue(cache.isEnabled());
            assertTrue(customStylesheet.isEnabled());
            assertEquals("Force", browserDownload.getSelectedItem());
            assertEquals("/tmp/browser cache", cache.getText());
            assertEquals("/tmp/custom print.css", customStylesheet.getText());

            SwingUtilities.invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    executable.setText("/tmp/Chrome");
                    renderer.setSelectedItem("wkhtmltopdf");
                }
            });
            assertEquals("", executable.getText());
            assertFalse(browserDownload.isEnabled());
            assertFalse(cache.isEnabled());
            assertFalse(customStylesheet.isEnabled());
            assertEquals("/tmp/custom print.css", customStylesheet.getText());

            SwingUtilities.invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    renderer.setSelectedItem("Chrome");
                }
            });
            assertTrue(browserDownload.isEnabled());
            assertTrue(cache.isEnabled());
            assertTrue(customStylesheet.isEnabled());

            final ActionListener[] rendererListeners = renderer.getActionListeners();
            SwingUtilities.invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    executable.setText("/tmp/Chrome");
                    for (ActionListener listener : rendererListeners) {
                        listener.actionPerformed(new ActionEvent(renderer, ActionEvent.ACTION_PERFORMED, "comboBoxChanged"));
                    }
                }
            });
            assertEquals("/tmp/Chrome", executable.getText());
            assertFalse(browserDownload.isEnabled());

            Rectangle maxBounds = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
            assertTrue(frame[0].getWidth() <= maxBounds.width);
            assertTrue(frame[0].getHeight() <= maxBounds.height);

            SwingUtilities.invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    frame[0].setVisible(true);
                }
            });
            assertTrue(frame[0].isShowing());
        } finally {
            SwingUtilities.invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    if (frame[0] != null) {
                        frame[0].dispose();
                    }
                }
            });
            System.setOut(originalOut);
            System.setErr(originalErr);
        }
    }

    private static JFrame findMainFrame() {
        for (Window window : Window.getWindows()) {
            if (window instanceof JFrame && "Email to PDF Converter".equals(((JFrame) window).getTitle())) {
                return (JFrame) window;
            }
        }
        return null;
    }

    private static JComboBox<?> comboWithItem(Container root, String item) {
        for (Component component : root.getComponents()) {
            if (component instanceof JComboBox) {
                JComboBox<?> combo = (JComboBox<?>) component;
                for (int i = 0; i < combo.getItemCount(); i++) {
                    if (item.equals(combo.getItemAt(i))) {
                        return combo;
                    }
                }
            }
            if (component instanceof Container) {
                JComboBox<?> combo = comboWithItem((Container) component, item);
                if (combo != null) {
                    return combo;
                }
            }
        }
        return null;
    }

    private static Component componentByName(Container root, String name) {
        for (Component component : root.getComponents()) {
            if (name.equals(component.getName())) {
                return component;
            }
            if (component instanceof Container) {
                Component found = componentByName((Container) component, name);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static boolean hasLabel(Container root, String text) {
        for (Component component : root.getComponents()) {
            if (component instanceof JLabel && text.equals(((JLabel) component).getText())) {
                return true;
            }
            if (component instanceof Container && hasLabel((Container) component, text)) {
                return true;
            }
        }
        return false;
    }
}
