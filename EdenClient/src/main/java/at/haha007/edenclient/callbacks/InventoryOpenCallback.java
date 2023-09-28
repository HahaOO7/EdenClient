package at.haha007.edenclient.callbacks;

import at.haha007.edenclient.utils.ContainerInfo;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

public interface InventoryOpenCallback {
    Event<InventoryOpenCallback> EVENT = EventFactory.createArrayBacked(InventoryOpenCallback.class,
            listeners -> (items) -> {
                for (InventoryOpenCallback listener : listeners) {
                    listener.open(items);
                }
            });

    void open(ContainerInfo items);
}
