package at.haha007.edenclient.callbacks;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;

public interface PlayerInteractBlockCallback {
    Event<PlayerInteractBlockCallback> EVENT = EventFactory.createArrayBacked(PlayerInteractBlockCallback.class,
            listeners -> (player, world, hand, hitResult) -> {
                ActionResult result = ActionResult.PASS;
                for (PlayerInteractBlockCallback listener : listeners) {
                    ActionResult r = listener.interact(player, world, hand, hitResult);
                    if (r != ActionResult.PASS) {
                        result = r;
                    }
                }
                return result;
            });

    ActionResult interact(ClientPlayerEntity player, ClientWorld world, Hand hand, BlockHitResult hitResult);
}
