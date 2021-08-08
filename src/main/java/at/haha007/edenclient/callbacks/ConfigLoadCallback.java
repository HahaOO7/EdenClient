package at.haha007.edenclient.callbacks;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.nbt.NbtCompound;

public interface ConfigLoadCallback {
    Event<ConfigLoadCallback> EVENT = EventFactory.createArrayBacked(ConfigLoadCallback.class,
            listeners -> (tag) -> {
                for (ConfigLoadCallback listener : listeners) {
                    listener.onLoad(tag);
                }
            });

    void onLoad(NbtCompound tag);
}
