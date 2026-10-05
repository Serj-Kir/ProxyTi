package proxyti.api.events;

import proxyti.BackendServer;
import proxyti.api.Event;

import java.net.SocketAddress;

/** Fired when a login session ends. */
public final class PlayerDisconnectEvent extends Event {
    private final SocketAddress remoteAddress;
    private final BackendServer targetServer;
    private final long durationMillis;

    public PlayerDisconnectEvent(SocketAddress remoteAddress, BackendServer targetServer, long durationMillis) {
        this.remoteAddress = remoteAddress;
        this.targetServer = targetServer;
        this.durationMillis = durationMillis;
    }

    public SocketAddress remoteAddress() {
        return remoteAddress;
    }

    public BackendServer targetServer() {
        return targetServer;
    }

    public long durationMillis() {
        return durationMillis;
    }
}