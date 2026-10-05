package proxyti.api;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * A small synchronous event bus. Listener failures are contained so one plugin
 * cannot break the proxy loop.
 */
public final class EventBus {
    private final Map<Class<?>, CopyOnWriteArrayList<Consumer<?>>> consumers = new ConcurrentHashMap<>();
    private final CopyOnWriteArrayList<AnnotatedListener> annotated = new CopyOnWriteArrayList<>();

    @SuppressWarnings("unchecked")
    public <E extends Event> void subscribe(Class<E> type, Consumer<E> handler) {
        consumers.computeIfAbsent(type, key -> new CopyOnWriteArrayList<>()).add(handler);
    }

    public <E extends Event> void unsubscribe(Class<E> type, Consumer<E> handler) {
        List<Consumer<?>> list = consumers.get(type);
        if (list != null) {
            list.remove(handler);
        }
    }

    public void register(Object listener) {
        for (Method method : listener.getClass().getMethods()) {
            EventHandler annotation = method.getAnnotation(EventHandler.class);
            if (annotation == null || method.getParameterCount() != 1
                    || !Event.class.isAssignableFrom(method.getParameterTypes()[0])) {
                continue;
            }
            method.setAccessible(true);
            annotated.add(new AnnotatedListener(annotation.priority(), listener, method));
        }
    }

    public void unregister(Object listener) {
        annotated.removeIf(entry -> entry.owner == listener);
    }

    @SuppressWarnings("unchecked")
    public <E extends Event> E fire(E event) {
        for (Consumer<?> consumer : consumers.getOrDefault(event.getClass(), new CopyOnWriteArrayList<>())) {
            try {
                ((Consumer<E>) consumer).accept(event);
            } catch (Throwable failure) {
                report(failure);
            }
        }

        List<AnnotatedListener> matching = new ArrayList<>();
        for (AnnotatedListener entry : annotated) {
            if (entry.method.getParameterTypes()[0].isInstance(event)) {
                matching.add(entry);
            }
        }
        matching.sort(Comparator.comparingInt((AnnotatedListener entry) -> entry.priority).reversed());
        for (AnnotatedListener entry : matching) {
            try {
                entry.method.invoke(entry.owner, event);
            } catch (InvocationTargetException e) {
                report(e.getCause() == null ? e : e.getCause());
            } catch (Throwable failure) {
                report(failure);
            }
        }
        return event;
    }

    private static void report(Throwable failure) {
        System.err.println("[ProxyTi] Event listener failed: " + failure);
        failure.printStackTrace(System.err);
    }

    private record AnnotatedListener(int priority, Object owner, Method method) {
    }
}