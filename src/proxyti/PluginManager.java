package proxyti;

import proxyti.api.EventBus;
import proxyti.api.Plugin;
import proxyti.api.PluginDescription;
import proxyti.api.PluginEventBus;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.jar.JarFile;

/**
 * Discovers {@code *.jar} files in the plugins directory, reads each
 * {@code plugin.properties} descriptor and loads the declared main class as a
 * child of the proxy classloader (so the shared API types stay identical).
 */
public final class PluginManager {
    private final ProxyServer proxy;
    private final Path directory;
    private final EventBus bus;
    private final List<LoadedPlugin> loaded = new ArrayList<>();

    public PluginManager(ProxyServer proxy) {
        this.proxy = proxy;
        this.directory = Path.of(proxy.config().pluginsDirectory);
        this.bus = proxy.events();
    }

    public List<PluginDescription> descriptions() {
        List<PluginDescription> result = new ArrayList<>();
        for (LoadedPlugin plugin : loaded) {
            result.add(plugin.description);
        }
        return result;
    }

    public void loadAndEnable() {
        try {
            Files.createDirectories(directory);
        } catch (IOException e) {
            Log.error("Unable to create plugins directory '%s': %s", directory, e.getMessage());
            return;
        }
        try (DirectoryStream<Path> jars = Files.newDirectoryStream(directory, "*.jar")) {
            for (Path jar : jars) {
                loadJar(jar);
            }
        } catch (IOException e) {
            Log.error("Unable to scan plugins directory '%s': %s", directory, e.getMessage());
        }
        for (LoadedPlugin plugin : loaded) {
            try {
                plugin.instance.onEnable();
            } catch (Throwable failure) {
                Log.error("Plugin '%s' failed to enable: %s", plugin.description.name(), failure);
            }
        }
        Log.info("Loaded %d plugin(s) from %s", loaded.size(), directory.toAbsolutePath());
    }

    private void loadJar(Path jar) {
        String fallbackName = stripExtension(jar.getFileName().toString());
        PluginDescription description;
        try {
            description = readDescription(jar, fallbackName);
        } catch (IOException e) {
            Log.warn("Skipping %s: %s", jar.getFileName(), e.getMessage());
            return;
        }
        if (description.main().isBlank()) {
            Log.warn("Skipping %s: missing 'main' entry in plugin.properties", jar.getFileName());
            return;
        }
        try {
            URLClassLoader loader = new URLClassLoader(new URL[]{jar.toUri().toURL()}, Plugin.class.getClassLoader());
            Class<?> type = Class.forName(description.main(), true, loader);
            if (!Plugin.class.isAssignableFrom(type)) {
                Log.warn("Skipping %s: %s does not implement proxyti.api.Plugin", jar.getFileName(), description.main());
                loader.close();
                return;
            }
            Plugin instance = (Plugin) type.getDeclaredConstructor().newInstance();
            Path dataFolder = directory.resolve(description.name());
            Files.createDirectories(dataFolder);
            PluginEventBus pluginBus = new PluginEventBus(bus);
            ProxyTiContext context = new ProxyTiContext(proxy, description, dataFolder, pluginBus);
            instance.onLoad(context);
            loaded.add(new LoadedPlugin(description, instance, loader, pluginBus));
            Log.info("Loaded plugin %s v%s (%s)", description.name(), description.version(), description.author());
        } catch (Throwable failure) {
            Log.error("Failed to load plugin %s: %s", jar.getFileName(), failure);
        }
    }

    private static PluginDescription readDescription(Path jar, String fallbackName) throws IOException {
        try (JarFile file = new JarFile(jar.toFile())) {
            var entry = file.getEntry("plugin.properties");
            if (entry == null) {
                throw new IOException("plugin.properties not found in jar root");
            }
            try (InputStream in = file.getInputStream(entry)) {
                Properties properties = new Properties();
                properties.load(in);
                return PluginDescription.from(properties, fallbackName);
            }
        }
    }

    public void disableAll() {
        for (int i = loaded.size() - 1; i >= 0; i--) {
            LoadedPlugin plugin = loaded.get(i);
            try {
                plugin.instance.onDisable();
            } catch (Throwable failure) {
                Log.warn("Plugin '%s' failed to disable: %s", plugin.description.name(), failure);
            }
            plugin.eventBus.clear();
            try {
                plugin.loader.close();
            } catch (IOException ignored) {
            }
        }
        loaded.clear();
    }

    private static String stripExtension(String name) {
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(0, dot) : name;
    }

    private record LoadedPlugin(PluginDescription description, Plugin instance,
                                URLClassLoader loader, PluginEventBus eventBus) {
    }
}