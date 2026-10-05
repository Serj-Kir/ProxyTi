package proxyti;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Writes a documented starter configuration when none exists yet. */
public final class DefaultConfig {
    private DefaultConfig() {
    }

    public static final String TEMPLATE = """
            # ProxyTi configuration
            # Generated automatically on first launch. Backends require no plugin,
            # mod, forwarding secret, or proxy registration.

            bind = 0.0.0.0:25565
            default-server = lobby

            # Answer server-list pings locally instead of passing them to a backend.
            intercept-status = false
            motd = ProxyTi - Java Edition proxy
            max-players = 100
            connect-timeout-ms = 5000

            # Optional: send a different host name in the backend handshake.
            rewrite-handshake-host = false
            rewrite-handshake-host-to = localhost

            # Directory scanned for plugin jars at startup.
            plugins-directory = plugins

            debug = false

            # Backend servers: name = host:port
            server.lobby = 127.0.0.1:25566
            server.survival = 127.0.0.1:25567

            # Route by the host name the client used. Point both DNS names at the proxy.
            forced-host.lobby.example.com = lobby
            forced-host.survival.example.com = survival
            """;

    public static void write(Path path) throws IOException {
        Path parent = path.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.writeString(path, TEMPLATE);
    }
}