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

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Test;

/**
 * TestClass.
 * @author Nick Russler
 */
public class StringReplacerTest {

    @Test
    public void replace_replaceSingleChar() throws Exception {
        String expected = "abcabcabc";
        String result;

        result = StringReplacer.replace("abdabdabd", Pattern.compile("d"), new StringReplacerCallback() {
            @Override
            public String replace(Matcher match) throws Exception {
                return "c";
            }
        });

        assertThat(expected, equalTo(result));
    }

    @Test
    public void replace_replaceMultipleChars() throws Exception {
        String expected = "test17test";
        String result;

        result = StringReplacer.replace("test269test", Pattern.compile("\\d+"), new StringReplacerCallback() {
            @Override
            public String replace(Matcher match) throws Exception {
                int x = 0;
                for (int i = 0; i < match.group().length(); i++) {
                    x += Integer.parseInt("" + match.group().charAt(i));
                }

                return "" + x;
            }
        });

        assertThat(expected, equalTo(result));
    }

    @Test
    public void replace_obeyNewlines() throws Exception {
        String expected = "ab\nc";
        String result;

        result = StringReplacer.replace(expected, Pattern.compile("a.*b.*c"), new StringReplacerCallback() {
            @Override
            public String replace(Matcher match) throws Exception {
                return "";
            }
        });

        assertThat(expected, equalTo(result));
    }
}
