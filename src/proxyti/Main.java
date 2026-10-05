package proxyti;

import java.nio.file.Files;
import java.nio.file.Path;

/** Entry point for ProxyTi. */
public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        Path configPath = Path.of(args.length == 0 ? "config.properties" : args[0]);
        try {
            if (!Files.isRegularFile(configPath)) {
                DefaultConfig.write(configPath);
                Log.info("No configuration found; created a default one at %s", configPath.toAbsolutePath());
            }
            Config config = Config.load(configPath);
            Log.setDebug(config.debug);
            Log.info("Starting ProxyTi %s (transparent Java Edition routing proxy)", ProxyServer.VERSION);

            ProxyServer proxy = new ProxyServer(config);
            Runtime.getRuntime().addShutdownHook(new Thread(proxy::close, "proxyti-shutdown"));

            PluginManager plugins = new PluginManager(proxy);
            proxy.setPlugins(plugins);
            plugins.loadAndEnable();

            try {
                proxy.run();
            } finally {
                plugins.disableAll();
                proxy.close();
            }
        } catch (Exception e) {
            Log.error("Startup failed: %s", e.getMessage());
            if (Boolean.getBoolean("proxyti.debug")) {
                e.printStackTrace(System.err);
            }
            System.exit(1);
        }
    }
}