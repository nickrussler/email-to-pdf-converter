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

import java.util.concurrent.atomic.AtomicReference;

/**
 * Receives progress updates while Chrome is acquired or downloaded.
 */
public final class BrowserDownloadProgress {
    public interface Listener {
        /**
         * @param message current activity
         * @param percent 0-100, or a negative value when the duration is unknown
         */
        void progress(String message, int percent);

        void finished();
    }

    private static final AtomicReference<Listener> LISTENER = new AtomicReference<>();

    private BrowserDownloadProgress() {
    }

    public static void setListener(Listener newListener) {
        LISTENER.set(newListener);
    }

    public static boolean installIfAbsent(Listener newListener) {
        return LISTENER.compareAndSet(null, newListener);
    }

    static void progress(String message, int percent) {
        Listener current = LISTENER.get();
        if (current != null) {
            current.progress(message, percent);
        }
    }

    static void finished() {
        Listener current = LISTENER.get();
        if (current != null) {
            current.finished();
        }
    }
}
