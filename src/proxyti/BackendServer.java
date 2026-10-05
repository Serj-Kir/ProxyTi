package proxyti;

/**
 * A single backend Minecraft server the proxy can route players to.
 */
public final class BackendServer {
    public final String name;
    public final String host;
    public final int port;

    public BackendServer(String name, String host, int port) {
        this.name = name;
        this.host = host;
        this.port = port;
    }

    @Override
    public String toString() {
        return name + " (" + host + ":" + port + ")";
    }
}