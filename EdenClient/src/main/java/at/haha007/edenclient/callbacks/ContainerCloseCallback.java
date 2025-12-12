package at.haha007.edenclient.callbacks;

import net.minecraft.world.item.ItemStack;

import java.util.List;

public interface ContainerCloseCallback {
    Event<ContainerCloseCallback> EVENT = new Event<>(
            listeners -> {
                return items -> {
                    for (ContainerCloseCallback listener : listeners) {
                        listener.close(items);
                    }
                };
            });

    void close(List<ItemStack> items);
}
