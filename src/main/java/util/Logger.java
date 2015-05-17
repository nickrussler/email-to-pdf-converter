/*
 * EML to PDF Converter
 * Copyright (C) 2015 Nick Russler
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package util;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Simple logging class.
 * @author Nick Russler
 * @see http://stackoverflow.com/a/4332163/441907
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
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd HH:mm,SSS");

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