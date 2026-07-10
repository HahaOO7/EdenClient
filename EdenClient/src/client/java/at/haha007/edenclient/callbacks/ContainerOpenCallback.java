package at.haha007.edenclient.callbacks;

import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.MenuType;

/**
 * Callback for when a container screen is about to be opened.
 * Return true to cancel the opening of the container.
 */
public interface ContainerOpenCallback {
    Event<ContainerOpenCallback> EVENT = new Event<>(
            listeners -> (type, id, title) -> {
                for (ContainerOpenCallback listener : listeners) {
                    if (listener.onContainerOpen(type, id, title)) {
                        return true;
                    }
                }
                return false;
            });

    /**
     * Called when a container screen is about to be opened.
     *
     * @param type  The type of menu being opened
     * @param id    The container ID
     * @param title The title of the container
     * @return true to cancel the opening of the container, false to allow it
     */
    boolean onContainerOpen(MenuType<?> type, int id, Component title);
}
