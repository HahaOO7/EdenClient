package at.haha007.edenclient.callbacks;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;

public interface PlayerInteractBlockEvent {
	Event<PlayerInteractBlockEvent> EVENT = EventFactory.createArrayBacked(PlayerInteractBlockEvent.class,
		listeners -> (player, world, hand, hitResult) -> {
			for (PlayerInteractBlockEvent listener : listeners) {
				ActionResult result = listener.interact(player, world, hand, hitResult);

				if (result != ActionResult.PASS) {
					return result;
				}
			}

			return ActionResult.PASS;
		});

	ActionResult interact(ClientPlayerEntity player, ClientWorld world, Hand hand, BlockHitResult hitResult);
}
