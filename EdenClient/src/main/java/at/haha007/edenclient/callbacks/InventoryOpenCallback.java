package at.haha007.edenclient.callbacks;

import at.haha007.edenclient.utils.ContainerInfo;

public interface InventoryOpenCallback {
    Event<InventoryOpenCallback> EVENT = new Event<>(
            listeners -> (items) -> {
                for (InventoryOpenCallback listener : listeners) {
                    listener.open(items);
                }
            });

    void open(ContainerInfo items);
}
