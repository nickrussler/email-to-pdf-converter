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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.net.InetSocketAddress;
import java.net.Proxy;
import org.junit.Test;

public class ProxyUrlTest {
    @Test
    public void formatsUnresolvedProxyAddressesWithoutAddressSuffix() {
        Proxy proxy = new Proxy(Proxy.Type.HTTP, InetSocketAddress.createUnresolved("127.0.0.1", 3128));

        assertEquals("http://127.0.0.1:3128", ProxyUrl.format(proxy));
        assertSameProxy(proxy, ProxyUrl.parse(ProxyUrl.format(proxy)));
    }

    @Test
    public void formatsResolvedProxyAddresses() {
        Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("localhost", 3128));

        assertEquals("http://localhost:3128", ProxyUrl.format(proxy));
        assertSameProxy(proxy, ProxyUrl.parse(ProxyUrl.format(proxy)));
    }

    @Test
    public void ignoresDirectProxy() {
        assertNull(ProxyUrl.format(Proxy.NO_PROXY));
    }

    @Test
    public void parsesHttpAndSocksProxySettings() {
        Proxy http = ProxyUrl.parse("http://127.0.0.1:3128");
        assertEquals(Proxy.Type.HTTP, http.type());
        assertEquals(new InetSocketAddress("127.0.0.1", 3128), http.address());

        Proxy socks = ProxyUrl.parse("socks5://127.0.0.1:1080");
        assertEquals(Proxy.Type.SOCKS, socks.type());
        assertEquals(new InetSocketAddress("127.0.0.1", 1080), socks.address());
    }

    private static void assertSameProxy(Proxy expected, Proxy actual) {
        assertEquals(expected.type(), actual.type());
        InetSocketAddress expectedAddress = (InetSocketAddress) expected.address();
        InetSocketAddress actualAddress = (InetSocketAddress) actual.address();
        assertEquals(expectedAddress.getHostString(), actualAddress.getHostString());
        assertEquals(expectedAddress.getPort(), actualAddress.getPort());
    }

    @Test
    public void normalizesBareHostAndPortForChrome() {
        assertEquals("http://proxy.example.com:3128", ProxyUrl.normalizeForChrome("proxy.example.com:3128"));
    }

    @Test
    public void preservesSupportedChromeProxySchemes() {
        assertEquals("socks5://proxy.example.com:1080", ProxyUrl.normalizeForChrome("socks5://proxy.example.com:1080"));
        assertEquals("socks5://proxy.example.com:1080", ProxyUrl.normalizeForChrome("socks://proxy.example.com:1080"));
        assertEquals("https://proxy.example.com:8443", ProxyUrl.normalizeForChrome("https://proxy.example.com:8443"));
    }

    @Test
    public void rejectsUnsupportedChromeProxySchemes() {
        try {
            ProxyUrl.normalizeForChrome("ftp://proxy.example.com:21");
            fail("Expected an unsupported proxy scheme error");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), containsString("unsupported proxy scheme"));
        }
    }

    @Test
    public void rejectsProxyCredentialsForChrome() {
        try {
            ProxyUrl.normalizeForChrome("http://user:password@127.0.0.1:3128");
            fail("Expected unsupported proxy authentication");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), containsString("Proxy authentication is not supported"));
        }
    }

    @Test
    public void rejectsProxyCredentials() {
        try {
            ProxyUrl.parse("http://user:password@127.0.0.1:3128");
            fail("Expected unsupported proxy authentication");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), containsString("Proxy authentication is not supported"));
        }
    }
}
