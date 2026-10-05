package proxyti.api;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * A per-plugin view of the {@link EventBus}. Everything registered through this
 * view is removed automatically when the plugin is disabled.
 */
public final class PluginEventBus {
    private final EventBus bus;
    private final List<Runnable> removers = new ArrayList<>();

    public PluginEventBus(EventBus bus) {
        this.bus = bus;
    }

    public <E extends Event> void subscribe(Class<E> type, Consumer<E> handler) {
        bus.subscribe(type, handler);
        removers.add(() -> bus.unsubscribe(type, handler));
    }

    public void register(Object listener) {
        bus.register(listener);
        removers.add(() -> bus.unregister(listener));
    }

    public void clear() {
        for (Runnable remover : removers) {
            remover.run();
        }
        removers.clear();
    }
}