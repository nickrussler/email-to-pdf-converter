/*
 * Copyright 2016 Nick Russler
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package util;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Simple logging class.
 * @author Nick Russler
 * @see <a href="http://stackoverflow.com/a/4332163/441907">http://stackoverflow.com/a/4332163/441907</a>
 */
public class Logger {
    public static LogLevel level = LogLevel.Info;

    /**
     * Log a string. Message is formatted with the supplied params using the String.format function.
     * @param message string message
     * @param params params to insert into the message
     */
    public static void debug(String message, Object... params) {
        if (level.compareTo(LogLevel.Debug) < 0) {
            return;
        }


        String fullClassName = Thread.currentThread().getStackTrace()[2].getClassName();
        String className = fullClassName.substring(fullClassName.lastIndexOf(".") + 1);
        String methodName = Thread.currentThread().getStackTrace()[2].getMethodName();
        int lineNumber = Thread.currentThread().getStackTrace()[2].getLineNumber();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd HH:mm,sss");

        System.out.println(sdf.format(new Date()) + " [" + className + "." + methodName + "()#" + lineNumber + "]: " + String.format(message, params));
    }

    /**
     * Log a string. Message is formatted with the supplied params using the String.format function.
     * @param message string message
     * @param params params to insert into the message
     */
    public static void info(String message, Object... params) {
        if (level.compareTo(LogLevel.Info) < 0) {
            return;
        }

        System.out.println(String.format(message, params));
    }

    /**
     * Log a string to the default error. Message is formatted with the supplied params using the String.format function.
     * @param message string message
     * @param params params to insert into the message
     */
    public static void error(String message, Object... params) {
        if (level.compareTo(LogLevel.Error) < 0) {
            return;
        }

        System.err.println(String.format(message, params));
    }
}
