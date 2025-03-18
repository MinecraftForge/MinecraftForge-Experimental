package net.minecraftforge.common.util;

// Temporary solution for events that rely on the cancellation state being stored inside the event object
public interface CancellableAware {
    default boolean isCanceled() {
        return false;
    }

    default void setCanceled(boolean cancelled) {}
}
