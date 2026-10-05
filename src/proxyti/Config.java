package proxyti;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/** Configuration deliberately uses a small .properties-like format. */
public final class Config {
    public String bindHost = "0.0.0.0";
    public int bindPort = 25565;
    public String defaultServer = "lobby";
    public boolean interceptStatus = false;
    public String motd = "ProxyTi";
    public int maxPlayers = 100;
    public int connectTimeoutMs = 5000;
    public boolean rewriteHandshakeHost = false;
    public String rewriteHandshakeHostTo = "localhost";
    public String pluginsDirectory = "plugins";
    public boolean debug = false;
    public final Map<String, BackendServer> servers = new LinkedHashMap<>();
    public final Map<String, String> forcedHosts = new LinkedHashMap<>();

    public static Config load(Path path) throws IOException {
        Config config = new Config();
        try (BufferedReader reader = Files.newBufferedReader(path)) {
            String raw;
            int line = 0;
            while ((raw = reader.readLine()) != null) {
                line++;
                String value = raw.trim();
                if (value.isEmpty() || value.startsWith("#") || value.startsWith(";")) {
                    continue;
                }
                int separator = value.indexOf('=');
                if (separator < 1) {
                    throw new IOException("Invalid configuration at " + path + ":" + line);
                }
                String key = value.substring(0, separator).trim().toLowerCase(Locale.ROOT);
                String setting = value.substring(separator + 1).trim();
                config.set(key, setting, path, line);
            }
        }
        config.validate();
        return config;
    }

    private void set(String key, String value, Path path, int line) throws IOException {
        if (key.equals("bind")) {
            HostPort address = HostPort.parse(value);
            bindHost = address.host;
            bindPort = address.port;
        } else if (key.equals("default-server")) {
            defaultServer = value.toLowerCase(Locale.ROOT);
        } else if (key.equals("intercept-status")) {
            interceptStatus = Boolean.parseBoolean(value);
        } else if (key.equals("motd")) {
            motd = value.replace("\\n", "\n");
        } else if (key.equals("max-players")) {
            maxPlayers = Integer.parseInt(value);
        } else if (key.equals("connect-timeout-ms")) {
            connectTimeoutMs = Integer.parseInt(value);
        } else if (key.equals("rewrite-handshake-host")) {
            rewriteHandshakeHost = Boolean.parseBoolean(value);
        } else if (key.equals("rewrite-handshake-host-to")) {
            rewriteHandshakeHostTo = value;
        } else if (key.equals("plugins-directory")) {
            pluginsDirectory = value.isBlank() ? "plugins" : value;
        } else if (key.equals("debug")) {
            debug = Boolean.parseBoolean(value);
        } else if (key.startsWith("server.")) {
            String name = key.substring("server.".length());
            if (name.isBlank()) {
                throw new IOException("Empty server name at " + path + ":" + line);
            }
            HostPort address = HostPort.parse(value);
            servers.put(name, new BackendServer(name, address.host, address.port));
        } else if (key.startsWith("forced-host.")) {
            String host = key.substring("forced-host.".length()).toLowerCase(Locale.ROOT);
            forcedHosts.put(normalizeHost(host), value.toLowerCase(Locale.ROOT));
        } else {
            Log.warn("Ignoring unknown configuration key '%s'", key);
        }
    }

    private void validate() throws IOException {
        if (bindPort < 1 || bindPort > 65535) {
            throw new IOException("bind port must be between 1 and 65535");
        }
        if (servers.isEmpty()) {
            throw new IOException("At least one server.<name>=host:port entry is required");
        }
        if (!servers.containsKey(defaultServer)) {
            throw new IOException("default-server '" + defaultServer + "' has no matching server entry");
        }
        for (Map.Entry<String, String> entry : forcedHosts.entrySet()) {
            if (!servers.containsKey(entry.getValue())) {
                throw new IOException("forced-host." + entry.getKey() + " refers to missing server '" + entry.getValue() + "'");
            }
        }
    }

    public BackendServer route(String requestedHost) {
        String selected = forcedHosts.getOrDefault(normalizeHost(requestedHost), defaultServer);
        return servers.get(selected);
    }

    public static String normalizeHost(String value) {
        String host = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        int nul = host.indexOf('\0');
        if (nul >= 0) {
            host = host.substring(0, nul);
        }
        if (host.endsWith(".")) {
            host = host.substring(0, host.length() - 1);
        }
        return host;
    }

    private record HostPort(String host, int port) {
        static HostPort parse(String input) throws IOException {
            String text = input.trim();
            if (text.startsWith("[")) {
                int close = text.indexOf(']');
                if (close < 0 || close + 2 >= text.length() || text.charAt(close + 1) != ':') {
                    throw new IOException("Invalid address: " + input);
                }
                return new HostPort(text.substring(1, close), parsePort(text.substring(close + 2), input));
            }
            int colon = text.lastIndexOf(':');
            if (colon <= 0 || colon == text.length() - 1) {
                throw new IOException("Expected host:port, got: " + input);
            }
            return new HostPort(text.substring(0, colon), parsePort(text.substring(colon + 1), input));
        }

        private static int parsePort(String value, String source) throws IOException {
            try {
                int port = Integer.parseInt(value);
                if (port < 1 || port > 65535) throw new NumberFormatException();
                return port;
            } catch (NumberFormatException ignored) {
                throw new IOException("Invalid port in " + source);
            }
        }
    }
}
