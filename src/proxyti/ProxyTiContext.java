package proxyti;

import proxyti.api.PluginDescription;
import proxyti.api.PluginEventBus;
import proxyti.api.ProxyTi;
import proxyti.api.ProxytiLogger;

import java.nio.file.Path;
import java.util.List;

/** Per-plugin implementation of the {@link ProxyTi} facade. */
final class ProxyTiContext implements ProxyTi {
    private final ProxyServer proxy;
    private final PluginDescription description;
    private final Path dataFolder;
    private final PluginEventBus events;
    private final ProxytiLogger logger;

    ProxyTiContext(ProxyServer proxy, PluginDescription description, Path dataFolder, PluginEventBus events) {
        this.proxy = proxy;
        this.description = description;
        this.dataFolder = dataFolder;
        this.events = events;
        this.logger = new ProxytiLogger(description.name());
    }

    @Override
    public String name() {
        return description.name();
    }

    @Override
    public ProxytiLogger logger() {
        return logger;
    }

    @Override
    public Path dataFolder() {
        return dataFolder;
    }

    @Override
    public Config config() {
        return proxy.config();
    }

    @Override
    public PluginEventBus events() {
        return events;
    }

    @Override
    public BackendServer server(String name) {
        return proxy.config().servers.get(name);
    }

    @Override
    public List<BackendServer> servers() {
        return proxy.servers();
    }

    @Override
    public int onlinePlayers() {
        return proxy.activeLogins();
    }

    @Override
    public String version() {
        return proxy.version();
    }
}