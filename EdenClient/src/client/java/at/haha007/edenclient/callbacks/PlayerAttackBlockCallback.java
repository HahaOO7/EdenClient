package at.haha007.edenclient.callbacks;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;

public interface PlayerAttackBlockCallback {
    Event<PlayerAttackBlockCallback> EVENT = new Event<>(
            listeners -> (player, pos, side) -> {
                InteractionResult result = InteractionResult.PASS;
                for (PlayerAttackBlockCallback listener : listeners) {
                    InteractionResult r = listener.interact(player, pos, side);
                    if (r != InteractionResult.PASS) {
                        result = r;
                    }
                }
                return result;
            });

    InteractionResult interact(LocalPlayer player, BlockPos pos, Direction side);
}
