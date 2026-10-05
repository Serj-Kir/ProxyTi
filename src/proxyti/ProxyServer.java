package proxyti;

import proxyti.api.EventBus;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/** The listener and routing state shared by all client sessions. */
public final class ProxyServer implements AutoCloseable {
    public static final String VERSION = "1.0.0";

    private final Config config;
    private final EventBus events = new EventBus();
    private final AtomicBoolean running = new AtomicBoolean();
    private final AtomicInteger activeLogins = new AtomicInteger();
    private final ExecutorService workers = Executors.newVirtualThreadPerTaskExecutor();
    private ServerSocket listener;
    private PluginManager plugins;

    public ProxyServer(Config config) {
        this.config = config;
    }

    public Config config() {
        return config;
    }

    public EventBus events() {
        return events;
    }

    public String version() {
        return VERSION;
    }

    public List<BackendServer> servers() {
        return new ArrayList<>(config.servers.values());
    }

    public int activeLogins() {
        return activeLogins.get();
    }

    public PluginManager plugins() {
        return plugins;
    }

    void setPlugins(PluginManager plugins) {
        this.plugins = plugins;
    }

    void loginOpened() {
        activeLogins.incrementAndGet();
    }

    void loginClosed() {
        activeLogins.updateAndGet(current -> Math.max(0, current - 1));
    }

    public void run() throws IOException {
        listener = new ServerSocket();
        listener.setReuseAddress(true);
        listener.bind(new InetSocketAddress(config.bindHost, config.bindPort));
        running.set(true);
        Log.info("ProxyTi %s is listening on %s:%d", VERSION, config.bindHost, config.bindPort);
        Log.info("Default backend: %s", config.route(""));

        while (running.get()) {
            try {
                Socket socket = listener.accept();
                socket.setTcpNoDelay(true);
                socket.setKeepAlive(true);
                workers.submit(new ClientHandler(this, socket));
            } catch (IOException e) {
                if (running.get()) {
                    Log.warn("Unable to accept connection: %s", e.getMessage());
                }
            }
        }
    }

    @Override
    public void close() {
        running.set(false);
        if (listener != null) {
            try {
                listener.close();
            } catch (IOException ignored) {
            }
        }
        workers.shutdownNow();
    }
}