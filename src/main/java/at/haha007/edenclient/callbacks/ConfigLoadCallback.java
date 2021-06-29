package at.haha007.edenclient.callbacks;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.ActionResult;

public interface ConfigLoadCallback {
    Event<ConfigLoadCallback> EVENT = EventFactory.createArrayBacked(ConfigLoadCallback.class,
            listeners -> (tag) -> {
                for (ConfigLoadCallback listener : listeners) {
                    ActionResult result = listener.onLoad(tag);

                    if (result != ActionResult.PASS) {
                        return result;
                    }
                }
                return ActionResult.PASS;
            });

    ActionResult onLoad(NbtCompound tag);
}
