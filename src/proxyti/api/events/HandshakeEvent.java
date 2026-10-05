package proxyti.api.events;

import proxyti.BackendServer;
import proxyti.api.Event;

import java.net.SocketAddress;

/**
 * Fired as soon as the handshake has been read, before any backend is
 * contacted. Cancelling it drops the connection.
 */
public final class HandshakeEvent extends Event {
    private final SocketAddress remoteAddress;
    private final int protocolVersion;
    private final String requestedHost;
    private final boolean status;
    private BackendServer targetServer;
    private boolean interceptStatus;

    public HandshakeEvent(SocketAddress remoteAddress, int protocolVersion, String requestedHost,
                          boolean status, BackendServer targetServer) {
        this.remoteAddress = remoteAddress;
        this.protocolVersion = protocolVersion;
        this.requestedHost = requestedHost;
        this.status = status;
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

    public boolean isStatus() {
        return status;
    }

    public BackendServer targetServer() {
        return targetServer;
    }

    /** Reroute this connection to another configured backend. */
    public void setTargetServer(BackendServer targetServer) {
        if (targetServer == null) {
            throw new IllegalArgumentException("targetServer must not be null");
        }
        this.targetServer = targetServer;
    }

    public boolean isInterceptStatus() {
        return interceptStatus;
    }

    /** Ask ProxyTi to answer a status ping itself, enabling StatusRequestEvent. */
    public void setInterceptStatus(boolean interceptStatus) {
        this.interceptStatus = interceptStatus;
    }

    @Override
    public boolean isCancellable() {
        return true;
    }
}