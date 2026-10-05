package proxyti.api;

/**
 * Entry point for a ProxyTi plugin. Implement this class, declare it in the
 * plugin's {@code plugin.properties} as {@code main}, and drop the resulting
 * jar into the {@code plugins/} directory.
 */
public interface Plugin {
    /** Called once immediately after the plugin is constructed. */
    default void onLoad(ProxyTi proxy) {
    }

    /** Called after every plugin has been loaded. */
    default void onEnable() {
    }

    /** Called on shutdown, before listeners are removed. */
    default void onDisable() {
    }
}