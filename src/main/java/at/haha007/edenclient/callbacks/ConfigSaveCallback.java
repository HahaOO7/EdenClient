package at.haha007.edenclient.callbacks;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.ActionResult;

public interface ConfigSaveCallback {
    Event<ConfigSaveCallback> EVENT = EventFactory.createArrayBacked(ConfigSaveCallback.class,
            listeners -> (tag) -> {
                for (ConfigSaveCallback listener : listeners) {
                    ActionResult result = listener.onSave(tag);

                    if (result != ActionResult.PASS) {
                        return result;
                    }
                }
                return ActionResult.PASS;
            });

    ActionResult onSave(NbtCompound tag);
}
