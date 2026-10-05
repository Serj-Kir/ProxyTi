package proxyti.api;

import proxyti.Log;

/** Logger scoped to a plugin name so output stays attributable. */
public final class ProxytiLogger {
    private final String name;

    public ProxytiLogger(String name) {
        this.name = name;
    }

    public void info(String message, Object... args) {
        Log.info("[" + name + "] " + message, args);
    }

    public void warn(String message, Object... args) {
        Log.warn("[" + name + "] " + message, args);
    }

    public void error(String message, Object... args) {
        Log.error("[" + name + "] " + message, args);
    }

    public void debug(String message, Object... args) {
        Log.debug("[" + name + "] " + message, args);
    }
}