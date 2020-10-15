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

import com.github.markusbernhardt.proxy.ProxySearch;
import com.github.markusbernhardt.proxy.ProxySearch.Strategy;
import com.google.common.base.Throwables;
import com.google.common.io.CharStreams;

import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.List;

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
