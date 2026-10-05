package proxyti.api.events;

import proxyti.BackendServer;
import proxyti.api.Event;

import java.net.SocketAddress;

/**
 * Fired for a login connection just before ProxyTi opens the backend socket.
 * Cancelling it disconnects the player. In transparent mode the username is not
 * available because the login is encrypted end-to-end.
 */
public final class PlayerConnectEvent extends Event {
    private final SocketAddress remoteAddress;
    private final int protocolVersion;
    private final String requestedHost;
    private BackendServer targetServer;

    public PlayerConnectEvent(SocketAddress remoteAddress, int protocolVersion, String requestedHost,
                              BackendServer targetServer) {
        this.remoteAddress = remoteAddress;
        this.protocolVersion = protocolVersion;
        this.requestedHost = requestedHost;
        this.targetServer = targetServer;
    }

    public SocketAddress remoteAddress() {
        return remoteAddress;
    }

    public int protocolVersion() {
        return protocolVersion;
    }

    public String requestedHost() {
        return requestedHost;
    }

    public BackendServer targetServer() {
        return targetServer;
    }

    public void setTargetServer(BackendServer targetServer) {
        if (targetServer == null) {
            throw new IllegalArgumentException("targetServer must not be null");
        }
        this.targetServer = targetServer;
    }

    @Override
    public boolean isCancellable() {
        return true;
    }
}