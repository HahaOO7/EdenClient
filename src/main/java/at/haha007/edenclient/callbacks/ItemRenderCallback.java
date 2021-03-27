package at.haha007.edenclient.callbacks;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.entity.ItemEntity;
import net.minecraft.util.ActionResult;

public interface ItemRenderCallback {
	Event<ItemRenderCallback> EVENT = EventFactory.createArrayBacked(ItemRenderCallback.class,
		listeners -> (item, yaw, tickDelta, light) -> {
			for (ItemRenderCallback listener : listeners) {
				ActionResult result = listener.renderItem(item, yaw, tickDelta, light);

				if (result != ActionResult.PASS) {
					return result;
				}
			}

			return ActionResult.PASS;
		});

	ActionResult renderItem(ItemEntity itemEntity,
	                        float yaw,
	                        float tickDelta,
	                        int light);
}
