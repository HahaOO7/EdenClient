package at.haha007.edenclient.callbacks;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.item.ItemStack;

import java.util.List;

public interface InventoryOpenCallback {
    Event<InventoryOpenCallback> EVENT = EventFactory.createArrayBacked(InventoryOpenCallback.class,
            listeners -> (items) -> {
                for (InventoryOpenCallback listener : listeners) {
                    listener.open(items);
                }
            });

    void open(List<ItemStack> items);
}
