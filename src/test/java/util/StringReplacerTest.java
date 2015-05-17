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

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

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
		String result = null;

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
		String result = null;
		
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
		String result = null;
		
		result = StringReplacer.replace(expected, Pattern.compile("a.*b.*c"), new StringReplacerCallback() {
			@Override
			public String replace(Matcher match) throws Exception {
				return "";
			}
		});
		
		assertThat(expected, equalTo(result));
	}
}
