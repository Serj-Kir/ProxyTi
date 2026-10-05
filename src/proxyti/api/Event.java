package proxyti.api;

/**
 * Base class for everything fired on the {@link EventBus}. Events that carry a
 * decision (routing, connection acceptance) are cancellable.
 */
public abstract class Event {
    private boolean cancelled;

    public boolean isCancelled() {
        return cancelled;
    }

    public void setCancelled(boolean value) {
        if (value && !isCancellable()) {
            throw new IllegalStateException(getClass().getSimpleName() + " cannot be cancelled");
        }
        this.cancelled = value;
    }

    public boolean isCancellable() {
        return false;
    }
}