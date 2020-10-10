package at.haha007.edenclient.callbacks;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.ActionResult;

public interface PlayerEditSignCallback {
	Event<PlayerEditSignCallback> EVENT = EventFactory.createArrayBacked(PlayerEditSignCallback.class,
		listeners -> (player, sign) -> {
			for (PlayerEditSignCallback listener : listeners) {
				ActionResult result = listener.interact(player, sign);

				if (result != ActionResult.PASS) {
					return result;
				}
			}

			return ActionResult.PASS;
		});

	ActionResult interact(ClientPlayerEntity player, SignBlockEntity sign);
}
