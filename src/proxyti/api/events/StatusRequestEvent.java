package proxyti.api.events;

import proxyti.BackendServer;
import proxyti.api.Event;

/**
 * Fired when ProxyTi answers a server-list ping itself. Plugins may rewrite the
 * description, version name and player counts.
 */
public final class StatusRequestEvent extends Event {
    private final int protocolVersion;
    private final String requestedHost;
    private final BackendServer targetServer;
    private String description;
    private String versionName;
    private int onlinePlayers;
    private int maxPlayers;
    private boolean hidePlayers;

    public StatusRequestEvent(int protocolVersion, String requestedHost, BackendServer targetServer,
                              String description, String versionName, int onlinePlayers, int maxPlayers) {
        this.protocolVersion = protocolVersion;
        this.requestedHost = requestedHost;
        this.targetServer = targetServer;
        this.description = description;
        this.versionName = versionName;
        this.onlinePlayers = onlinePlayers;
        this.maxPlayers = maxPlayers;
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

    public String description() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String versionName() {
        return versionName;
    }

    public void setVersionName(String versionName) {
        this.versionName = versionName;
    }

    public int onlinePlayers() {
        return onlinePlayers;
    }

    public void setOnlinePlayers(int onlinePlayers) {
        this.onlinePlayers = onlinePlayers;
    }

    public int maxPlayers() {
        return maxPlayers;
    }

    public void setMaxPlayers(int maxPlayers) {
        this.maxPlayers = maxPlayers;
    }

    public boolean isHidePlayers() {
        return hidePlayers;
    }

    public void setHidePlayers(boolean hidePlayers) {
        this.hidePlayers = hidePlayers;
    }
}