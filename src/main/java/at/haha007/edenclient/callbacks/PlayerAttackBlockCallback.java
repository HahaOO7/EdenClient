package at.haha007.edenclient.callbacks;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public interface PlayerAttackBlockCallback {
    Event<PlayerAttackBlockCallback> EVENT = EventFactory.createArrayBacked(PlayerAttackBlockCallback.class,
            listeners -> (player, pos, side) -> {
                ActionResult result = ActionResult.PASS;
                for (PlayerAttackBlockCallback listener : listeners) {
                    ActionResult r = listener.interact(player, pos, side);
                    if (r != ActionResult.PASS) {
                        result = r;
                    }
                }
                return result;
            });

    ActionResult interact(ClientPlayerEntity player, BlockPos pos, Direction side);
}
