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

import java.io.PrintStream;
import java.util.Locale;
import render.chrome.BrowserDownloadProgress;
import util.LogLevel;
import util.Logger;

/**
 * Draws the browser acquisition progress on an interactive terminal.
 */
final class CliBrowserDownloadProgress implements BrowserDownloadProgress.Listener {
    private static final int BAR_WIDTH = 24;
    private static final int LOG_STEP = 20;

    private final PrintStream output;
    private final boolean terminal;
    private int lastLineLength;
    private int lastLoggedPercent = -1;
    private int nextLogPercent;
    private boolean lineDrawn;

    CliBrowserDownloadProgress(PrintStream output, boolean terminal) {
        this.output = output;
        this.terminal = terminal;
    }

    static CliBrowserDownloadProgress installIfSupported() {
        if (Logger.level.compareTo(LogLevel.Info) < 0) {
            return null;
        }

        CliBrowserDownloadProgress progress = new CliBrowserDownloadProgress(System.err, System.console() != null);
        return BrowserDownloadProgress.installIfAbsent(progress) ? progress : null;
    }

    @Override
    public void progress(String message, int percent) {
        if (terminal) {
            drawTerminalProgress(message, percent);
        } else {
            logProgress(message, percent);
        }
    }

    @Override
    public void finished() {
        clear();
        output.println("Chrome headless shell ready.");
        output.flush();
    }

    void clear() {
        if (!terminal || !lineDrawn) {
            return;
        }
        output.print('\r');
        for (int i = 0; i < lastLineLength; i++) {
            output.print(' ');
        }
        output.print('\r');
        output.flush();
        lineDrawn = false;
        lastLineLength = 0;
    }

    private void drawTerminalProgress(String message, int percent) {
        if (percent < 0) {
            drawTerminalLine(message + "...");
            return;
        }

        int filled = (int) Math.round(percent * BAR_WIDTH / 100d);
        StringBuilder bar = new StringBuilder(BAR_WIDTH);
        for (int i = 0; i < BAR_WIDTH; i++) {
            bar.append(i < filled ? '#' : '-');
        }
        drawTerminalLine(String.format(Locale.ROOT, "%s [%s] %3d%%", message, bar, percent));
    }

    private void logProgress(String message, int percent) {
        if (percent < 0) {
            output.println(message + "...");
            output.flush();
            return;
        }

        if (percent < lastLoggedPercent) {
            lastLoggedPercent = -1;
            nextLogPercent = 0;
        }
        if (percent >= nextLogPercent) {
            output.println(String.format(Locale.ROOT, "%s %d%%", message, percent));
            output.flush();
            lastLoggedPercent = percent;
            while (nextLogPercent <= percent && nextLogPercent < 100) {
                nextLogPercent += LOG_STEP;
            }
        }
    }

    private void drawTerminalLine(String line) {
        output.print('\r');
        output.print(line);
        if (line.length() < lastLineLength) {
            for (int i = line.length(); i < lastLineLength; i++) {
                output.print(' ');
            }
        }
        output.flush();
        lastLineLength = line.length();
        lineDrawn = true;
    }
}
