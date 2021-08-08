package at.haha007.edenclient.callbacks;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.nbt.NbtCompound;

public interface ConfigSaveCallback {
    Event<ConfigSaveCallback> EVENT = EventFactory.createArrayBacked(ConfigSaveCallback.class,
            listeners -> (tag) -> {
                for (ConfigSaveCallback listener : listeners) {
                    listener.onSave(tag);
                }
            });

    void onSave(NbtCompound tag);
}
