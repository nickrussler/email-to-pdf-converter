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

package render.chrome;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.junit.Test;

public class ChromeCdpClientTest {
    @Test
    public void loadTimeoutReturnsFalseWithoutClosingTheConnection() throws Exception {
        final CountDownLatch started = new CountDownLatch(1);
        WebSocketServer server = new WebSocketServer(new InetSocketAddress("127.0.0.1", 0)) {
            @Override
            public void onOpen(WebSocket connection, ClientHandshake handshake) {
            }

            @Override
            public void onClose(WebSocket connection, int code, String reason, boolean remote) {
            }

            @Override
            public void onMessage(WebSocket connection, String message) {
                JsonObject request = JsonParser.parseString(message).getAsJsonObject();
                if (!request.has("id")) {
                    return;
                }
                JsonObject response = new JsonObject();
                response.addProperty("id", request.get("id").getAsLong());
                response.add("result", new JsonObject());
                connection.send(response.toString());
            }

            @Override
            public void onError(WebSocket connection, Exception exception) {
            }

            @Override
            public void onStart() {
                started.countDown();
            }
        };

        server.start();
        assertTrue("WebSocket server did not start", started.await(5, TimeUnit.SECONDS));

        ChromeCdpClient client = null;
        try {
            client = new ChromeCdpClient(new URI("ws://127.0.0.1:" + server.getPort() + "/"), 100);
            client.connectBlocking(5, TimeUnit.SECONDS);
            client.prepareLoad();

            JsonObject params = new JsonObject();
            params.addProperty("frameId", "test-frame");
            params.addProperty("html", "<html><body>test</body></html>");
            client.sendCommand("Page.setDocumentContent", params);

            assertFalse("Expected the load wait to time out", client.awaitLoad());
            assertTrue("The connection should remain open after a load timeout", client.isOpen());
        } finally {
            if (client != null) {
                client.close();
            }
            server.stop(1000);
        }
    }

    @Test
    public void disconnectDuringLoadFailsImmediatelyWithReason() throws Exception {
        final CountDownLatch started = new CountDownLatch(1);
        WebSocketServer server = new WebSocketServer(new InetSocketAddress("127.0.0.1", 0)) {
            @Override
            public void onOpen(WebSocket connection, ClientHandshake handshake) {
            }

            @Override
            public void onClose(WebSocket connection, int code, String reason, boolean remote) {
            }

            @Override
            public void onMessage(final WebSocket connection, String message) {
                JsonObject request = JsonParser.parseString(message).getAsJsonObject();
                if (!request.has("id")) {
                    return;
                }
                JsonObject response = new JsonObject();
                response.addProperty("id", request.get("id").getAsLong());
                response.add("result", new JsonObject());
                connection.send(response.toString());
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Thread.sleep(200);
                        } catch (InterruptedException ignored) {
                            Thread.currentThread().interrupt();
                        }
                        connection.close(1001, "TEST_BROWSER_CRASH");
                    }
                }).start();
            }

            @Override
            public void onError(WebSocket connection, Exception exception) {
            }

            @Override
            public void onStart() {
                started.countDown();
            }
        };

        server.start();
        assertTrue("WebSocket server did not start", started.await(5, TimeUnit.SECONDS));

        ChromeCdpClient client = null;
        try {
            client = new ChromeCdpClient(new URI("ws://127.0.0.1:" + server.getPort() + "/"));
            client.connectBlocking(5, TimeUnit.SECONDS);
            client.prepareLoad();

            JsonObject params = new JsonObject();
            params.addProperty("frameId", "test-frame");
            params.addProperty("html", "<html><body>test</body></html>");
            client.sendCommand("Page.setDocumentContent", params);

            long startedAt = System.nanoTime();
            try {
                client.awaitLoad();
                fail("Expected the disconnect to fail the load wait");
            } catch (IOException e) {
                assertThat(e.getMessage(), containsString("TEST_BROWSER_CRASH"));
            }
            long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);
            assertTrue("Disconnect took too long to propagate: " + elapsedMillis + "ms", elapsedMillis < 5000);
        } finally {
            if (client != null) {
                client.close();
            }
            server.stop(1000);
        }
    }
}
