package at.haha007.edenclient.callbacks;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ActionResult;

public interface PerWorldConfigReloadCallback {
	Event<PerWorldConfigReloadCallback> EVENT = EventFactory.createArrayBacked(PerWorldConfigReloadCallback.class,
		listeners -> (tag) -> {
			for (PerWorldConfigReloadCallback listener : listeners) {
				ActionResult result = listener.reload(tag);

				if (result != ActionResult.PASS) {
					return result;
				}
			}
			return ActionResult.PASS;
		});

	ActionResult reload(CompoundTag tag);
}
