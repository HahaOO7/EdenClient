package at.haha007.edenclient.callbacks;


import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;

public interface InventoryKeyCallback {
    Event<InventoryKeyCallback> EVENT = new Event<>(
            listeners -> (event, screen) -> {
                for (InventoryKeyCallback listener : listeners) {
                    if (listener.onKeyEvent(event, screen)) {
                        return true;
                    }
                }
                return false;
            });

    /**
     * Returns true if the key event was consumed by the callback.
     * If the key event was consumed, the callback will not be called again.
     *
     * @param event  The key event
     * @param screen The inventory screen
     * @return true if the key event was consumed by the callback
     */
    boolean onKeyEvent(KeyEvent event, AbstractContainerScreen<?> screen);
}
