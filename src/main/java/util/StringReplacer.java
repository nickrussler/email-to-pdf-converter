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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Simple class to replace substrings with a callback function.
 * @author Nick Russler
 */
public class StringReplacer {
	/**
	 * Replaces occurences of the regex using a callback function.
	 * @param input String to modify
	 * @param regex regex
	 * @param callback the occurences of the regex are replaced with the result of the callback function
	 * @return modified string
	 * @throws Exception
	 */
	public static String replace(String input, Pattern regex, StringReplacerCallback callback) throws Exception {
		StringBuffer resultString = new StringBuffer();
		Matcher regexMatcher = regex.matcher(input);
		while (regexMatcher.find()) {
			regexMatcher.appendReplacement(resultString, Matcher.quoteReplacement(callback.replace(regexMatcher)));
		}
		regexMatcher.appendTail(resultString);

		return resultString.toString();
	}
}