package at.haha007.edenclient.callbacks;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public interface ContainerCloseCallback {
    Event<ContainerCloseCallback> EVENT = EventFactory.createArrayBacked(ContainerCloseCallback.class,
            listeners -> (items) -> {
                for (ContainerCloseCallback listener : listeners) {
                    listener.close(items);
                }
            });

    void close(List<ItemStack> items);
}
