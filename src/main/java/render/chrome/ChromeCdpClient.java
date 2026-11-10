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

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

/**
 * Minimal Chrome DevTools Protocol client used by the Chrome renderer.
 */
final class ChromeCdpClient extends WebSocketClient {
    private static final long COMMAND_TIMEOUT_MS = 30000;
    static final long LOAD_TIMEOUT_MS = 30000;

    private final long loadTimeoutMs;
    private final AtomicLong nextId = new AtomicLong();
    private final Map<Long, CompletableFuture<JsonObject>> pending = new ConcurrentHashMap<>();
    private volatile CompletableFuture<Void> loadFuture = CompletableFuture.completedFuture(null);

    ChromeCdpClient(URI serverUri) {
        this(serverUri, LOAD_TIMEOUT_MS);
    }

    ChromeCdpClient(URI serverUri, long loadTimeoutMs) {
        super(serverUri);
        this.loadTimeoutMs = loadTimeoutMs;
    }

    void prepareLoad() {
        loadFuture = new CompletableFuture<>();
    }

    boolean awaitLoad() throws IOException {
        CompletableFuture<Void> future = loadFuture;
        try {
            future.get(loadTimeoutMs, TimeUnit.MILLISECONDS);
            return true;
        } catch (TimeoutException e) {
            return false;
        } catch (ExecutionException e) {
            Throwable cause = e.getCause() == null ? e : e.getCause();
            throw new IOException("Chrome disconnected while loading the document: " + cause.getMessage(), cause);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while waiting for Chrome to finish loading the document", e);
        }
    }

    JsonObject sendCommand(String method, JsonObject params) throws Exception {
        long id = nextId.incrementAndGet();
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        pending.put(id, future);

        JsonObject message = new JsonObject();
        message.addProperty("id", id);
        message.addProperty("method", method);
        message.add("params", params);
        send(message.toString());

        try {
            JsonObject response = future.get(COMMAND_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            if (response.has("error")) {
                throw new IOException("CDP " + method + " failed: " + response.get("error"));
            }
            return response.has("result") ? response.getAsJsonObject("result") : new JsonObject();
        } finally {
            pending.remove(id);
        }
    }

    JsonObject sendCommand(String method) throws Exception {
        return sendCommand(method, new JsonObject());
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
    }

    @Override
    public void onMessage(String message) {
        JsonObject object = JsonParser.parseString(message).getAsJsonObject();
        if (object.has("id") && object.get("id").isJsonPrimitive()) {
            long id = object.get("id").getAsLong();
            CompletableFuture<JsonObject> future = pending.get(id);
            if (future != null) {
                future.complete(object);
            }
        } else if (object.has("method") && "Page.loadEventFired".equals(object.get("method").getAsString())) {
            loadFuture.complete(null);
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        IOException error = new IOException("CDP connection closed: " + reason);
        loadFuture.completeExceptionally(error);
        for (CompletableFuture<JsonObject> future : pending.values()) {
            future.completeExceptionally(error);
        }
    }

    @Override
    public void onError(Exception ex) {
        loadFuture.completeExceptionally(ex);
        for (CompletableFuture<JsonObject> future : pending.values()) {
            future.completeExceptionally(ex);
        }
    }
}
