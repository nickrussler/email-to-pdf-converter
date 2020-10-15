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
