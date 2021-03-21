package at.haha007.edenclient.callbacks;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.ActionResult;

public interface PlayerInvChangeCallback {
	Event<PlayerInvChangeCallback> EVENT = EventFactory.createArrayBacked(PlayerInvChangeCallback.class,
		listeners -> (inv) -> {
			for (PlayerInvChangeCallback listener : listeners) {
				ActionResult result = listener.onInvChange(inv);

				if (result != ActionResult.PASS) {
					return result;
				}
			}

			return ActionResult.PASS;
		});

	ActionResult onInvChange(PlayerInventory inv);
}
