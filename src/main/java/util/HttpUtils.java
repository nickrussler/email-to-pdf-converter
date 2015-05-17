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

import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;

import com.btr.proxy.search.ProxySearch;
import com.btr.proxy.search.ProxySearch.Strategy;
import com.google.common.base.Throwables;
import com.google.common.io.CharStreams;

/**
 * Utility Class for HTTP Request.
 * @author Nick Russler
 */
public class HttpUtils {
	/**
	 * Find the default proxy.
	 * @return default proxy
	 */
	public static Proxy getDefaultProxy() {
		ProxySearch proxySearch = new ProxySearch();
		proxySearch.addStrategy(Strategy.OS_DEFAULT);
		proxySearch.addStrategy(Strategy.JAVA);
		proxySearch.addStrategy(Strategy.BROWSER);
		ProxySelector proxySelector = proxySearch.getProxySelector();

		ProxySelector.setDefault(proxySelector);
		URI home = URI.create("http://www.google.com");
		List<Proxy> proxyList = proxySelector.select(home);
		if (proxyList != null && !proxyList.isEmpty()) {
			for (Proxy proxy : proxyList) {
				return proxy;
			}
		}

		return null;
	}

	/**
	 * Makes a HTTP Post.
	 * @param params
	 * @return Response string
	 */
	public static String postRequest(String url, String params) {
		try {
			URL urlObj = new URL(url);
			HttpURLConnection conn = (HttpURLConnection) urlObj.openConnection(getDefaultProxy());
			conn.setDoOutput(true);

			OutputStreamWriter writer = new OutputStreamWriter(conn.getOutputStream(), StandardCharsets.UTF_8);
			writer.write(params);
			writer.close();

			return CharStreams.toString(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
		} catch (Exception e) {
			return "ERROR: " + Throwables.getStackTraceAsString(e);
		}
	}
	
	/**
	 * Makes a HTTP Get.
	 * @return Response string
	 */
	public static String getRequest(String url) {
		try {
			URL urlObj = new URL(url);
			HttpURLConnection conn = (HttpURLConnection) urlObj.openConnection(getDefaultProxy());
			conn.setDoOutput(true);

			return CharStreams.toString(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
		} catch (Exception e) {
			return "ERROR: " + Throwables.getStackTraceAsString(e);
		}
	}
}
