package proxyti.api;

import proxyti.BackendServer;
import proxyti.Config;

import java.nio.file.Path;
import java.util.List;

/** The facade handed to every plugin. */
public interface ProxyTi {
    String name();

    ProxytiLogger logger();

    Path dataFolder();

    Config config();

    PluginEventBus events();

    BackendServer server(String name);

    List<BackendServer> servers();

    int onlinePlayers();

    String version();
}