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

package util;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URI;
import java.util.Locale;

/**
 * Formats and parses proxy settings used by the command line and browser downloader.
 */
public final class ProxyUrl {
    private ProxyUrl() {
    }

    public static String format(Proxy proxy) {
        if (proxy == null || proxy.type() == Proxy.Type.DIRECT || !(proxy.address() instanceof InetSocketAddress)) {
            return null;
        }
        InetSocketAddress address = (InetSocketAddress) proxy.address();
        String host = address.getHostString();
        if (host == null || host.isEmpty()) {
            return null;
        }
        if (host.indexOf(':') >= 0 && !host.startsWith("[")) {
            host = "[" + host + "]";
        }
        String port = address.getPort() >= 0 ? ":" + address.getPort() : "";
        return proxy.type().toString().toLowerCase(Locale.ROOT) + "://" + host + port;
    }

    public static Proxy parse(String value) {
        if (value == null || value.trim().isEmpty()) {
            return Proxy.NO_PROXY;
        }
        try {
            String normalized = value.contains("://") ? value.trim() : "http://" + value.trim();
            URI uri = URI.create(normalized);
            if (uri.getHost() == null) {
                throw new IllegalArgumentException("missing host");
            }
            if (uri.getRawUserInfo() != null && !uri.getRawUserInfo().isEmpty()) {
                throw new IllegalArgumentException("Proxy authentication is not supported for Chrome browser downloads; remove credentials from --proxy or use an installed browser.");
            }
            int port = uri.getPort();
            if (port < 0) {
                port = "https".equalsIgnoreCase(uri.getScheme()) ? 443 : 80;
            }
            Proxy.Type type = uri.getScheme() != null && uri.getScheme().toLowerCase(Locale.ROOT).startsWith("socks") ? Proxy.Type.SOCKS : Proxy.Type.HTTP;
            return new Proxy(type, new InetSocketAddress(uri.getHost(), port));
        } catch (IllegalArgumentException e) {
            if (e.getMessage() != null && e.getMessage().contains("Proxy authentication")) {
                throw e;
            }
            throw new IllegalArgumentException("Invalid proxy for Chrome browser download: " + value, e);
        }
    }
}
