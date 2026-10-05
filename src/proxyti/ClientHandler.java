package proxyti;

import proxyti.api.events.HandshakeEvent;
import proxyti.api.events.PlayerConnectEvent;
import proxyti.api.events.PlayerDisconnectEvent;
import proxyti.api.events.StatusRequestEvent;
import proxyti.protocol.Handshake;
import proxyti.protocol.Packet;
import proxyti.protocol.Protocol;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PushbackInputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.concurrent.CountDownLatch;

/** One client connection: parse one handshake, pick a backend, then relay. */
final class ClientHandler implements Runnable {
    private final ProxyServer proxy;
    private final Socket client;
    private Socket backend;
    private boolean countedLogin;
    private BackendServer routedServer;
    private long startedAt;

    ClientHandler(ProxyServer proxy, Socket client) {
        this.proxy = proxy;
        this.client = client;
    }

    @Override
    public void run() {
        try (client) {
            startedAt = System.nanoTime();
            SocketAddress remote = client.getRemoteSocketAddress();
            PushbackInputStream clientIn = new PushbackInputStream(client.getInputStream(), 1);
            int first = clientIn.read();
            if (first < 0) {
                return;
            }

            if (first == 0xFE) {
                routeLegacyPing(clientIn, first, remote);
                return;
            }
            clientIn.unread(first);

            Handshake handshake = Handshake.read(clientIn);
            BackendServer target = proxy.config().route(handshake.host);

            HandshakeEvent event = new HandshakeEvent(remote, handshake.protocolVersion, handshake.host,
                    handshake.isStatus(), target);
            proxy.events().fire(event);
            if (event.isCancelled()) {
                Log.debug("Connection from %s was denied by a plugin", remote);
                return;
            }
            target = event.targetServer();
            routedServer = target;
            if (target == null) {
                Log.warn("No backend for %s", handshake.host);
                return;
            }

            Log.info("%s requested %s -> %s", remote, Config.normalizeHost(handshake.host), target);

            if (handshake.isStatus() && (proxy.config().interceptStatus || event.isInterceptStatus())) {
                answerStatus(clientIn, client.getOutputStream(), handshake, target);
                return;
            }
            if (!handshake.isStatus() && !handshake.isLogin()) {
                Log.warn("Unsupported handshake intent %d from %s", handshake.nextState, remote);
                return;
            }

            if (handshake.isLogin()) {
                PlayerConnectEvent connect = new PlayerConnectEvent(remote, handshake.protocolVersion,
                        handshake.host, target);
                proxy.events().fire(connect);
                if (connect.isCancelled()) {
                    Log.debug("Login from %s was denied by a plugin", remote);
                    return;
                }
                target = connect.targetServer();
                routedServer = target;
            }

            connect(target);
            String originalHost = handshake.host;
            if (proxy.config().rewriteHandshakeHost) {
                handshake.host = proxy.config().rewriteHandshakeHostTo;
            }
            Protocol.writeFrame(backend.getOutputStream(), handshake.encode(handshake.nextState));
            handshake.host = originalHost;

            if (handshake.isLogin()) {
                countedLogin = true;
                proxy.loginOpened();
            }
            relay(clientIn, client.getOutputStream(), backend.getInputStream(), backend.getOutputStream());
        } catch (IOException e) {
            Log.debug("Connection %s ended: %s", client.getRemoteSocketAddress(), e.getMessage());
        } finally {
            closeBackend();
            if (countedLogin) {
                proxy.loginClosed();
            }
            if (routedServer != null) {
                long duration = (System.nanoTime() - startedAt) / 1_000_000L;
                proxy.events().fire(new PlayerDisconnectEvent(client.getRemoteSocketAddress(), routedServer, duration));
            }
        }
    }

    /** Preserve an old pre-1.7 ping exactly; it cannot be host-routed. */
    private void routeLegacyPing(PushbackInputStream clientIn, int firstByte, SocketAddress remote) throws IOException {
        BackendServer target = proxy.config().route("");
        routedServer = target;
        Log.debug("Legacy ping from %s -> %s", remote, target);
        connect(target);
        OutputStream backendOut = backend.getOutputStream();
        backendOut.write(firstByte);
        backendOut.flush();
        relay(clientIn, client.getOutputStream(), backend.getInputStream(), backendOut);
    }

    private void connect(BackendServer target) throws IOException {
        backend = new Socket();
        backend.setTcpNoDelay(true);
        backend.setKeepAlive(true);
        backend.connect(new InetSocketAddress(target.host, target.port), proxy.config().connectTimeoutMs);
    }

    /**
     * A transparent byte relay. The client and backend negotiate encryption and
     * compression directly with one another; ProxyTi never sees their payload.
     */
    private void relay(InputStream clientIn, OutputStream clientOut, InputStream backendIn, OutputStream backendOut) {
        CountDownLatch finished = new CountDownLatch(2);
        Thread toBackend = Thread.ofVirtual().name("proxyti-client-to-backend").start(
                () -> copy(clientIn, backendOut, backend, finished));
        Thread toClient = Thread.ofVirtual().name("proxyti-backend-to-client").start(
                () -> copy(backendIn, clientOut, client, finished));
        try {
            finished.await();
            toBackend.join();
            toClient.join();
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }

    private static void copy(InputStream from, OutputStream to, Socket receivingSocket, CountDownLatch finished) {
        byte[] buffer = new byte[16 * 1024];
        try {
            int read;
            while ((read = from.read(buffer)) >= 0) {
                to.write(buffer, 0, read);
                to.flush();
            }
            try {
                receivingSocket.shutdownOutput();
            } catch (IOException ignored) {
            }
        } catch (IOException ignored) {
            try {
                receivingSocket.shutdownOutput();
            } catch (IOException ignoredAgain) {
            }
        } finally {
            finished.countDown();
        }
    }

    /** Implements the standard status request + ping exchange locally. */
    private void answerStatus(InputStream in, OutputStream out, Handshake handshake, BackendServer target)
            throws IOException {
        byte[] requestFrame = Protocol.readFrame(in);
        Packet request = Packet.decode(requestFrame);
        if (request.id != 0x00 || request.payload.length != 0) {
            throw new IOException("Invalid status request");
        }

        StatusRequestEvent event = new StatusRequestEvent(handshake.protocolVersion, handshake.host, target,
                proxy.config().motd, "ProxyTi", proxy.activeLogins(), proxy.config().maxPlayers);
        proxy.events().fire(event);

        int online = event.isHidePlayers() ? 0 : event.onlinePlayers();
        int max = event.isHidePlayers() ? 0 : event.maxPlayers();
        String json = "{\"version\":{\"name\":\"" + jsonEscape(event.versionName()) + "\",\"protocol\":"
                + handshake.protocolVersion + "},"
                + "\"players\":{\"max\":" + max + ",\"online\":" + online + ",\"sample\":[]},"
                + "\"description\":{\"text\":\"" + jsonEscape(event.description()) + "\"}}";
        ByteArrayOutputStream response = new ByteArrayOutputStream();
        Protocol.writeVarInt(response, 0x00);
        Protocol.writeString(response, json);
        Protocol.writeFrame(out, response.toByteArray());

        // Vanilla immediately sends a ping; echo its complete payload as Pong.
        byte[] pingFrame = Protocol.readFrame(in);
        Packet ping = Packet.decode(pingFrame);
        if (ping.id == 0x01 && ping.payload.length == 8) {
            ByteArrayOutputStream pong = new ByteArrayOutputStream(9);
            Protocol.writeVarInt(pong, 0x01);
            pong.write(ping.payload);
            Protocol.writeFrame(out, pong.toByteArray());
        }
    }

    private static String jsonEscape(String text) {
        String value = text == null ? "" : text;
        StringBuilder escaped = new StringBuilder(value.length() + 8);
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"' -> escaped.append("\\\"");
                case '\\' -> escaped.append("\\\\");
                case '\n' -> escaped.append("\\n");
                case '\r' -> escaped.append("\\r");
                case '\t' -> escaped.append("\\t");
                default -> {
                    if (c < 0x20) {
                        escaped.append(String.format("\\u%04x", (int) c));
                    } else {
                        escaped.append(c);
                    }
                }
            }
        }
        return escaped.toString();
    }

    private void closeBackend() {
        if (backend != null) {
            try {
                backend.close();
            } catch (IOException ignored) {
            }
        }
    }
}