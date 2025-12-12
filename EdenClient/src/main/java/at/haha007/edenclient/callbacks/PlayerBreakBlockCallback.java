package at.haha007.edenclient.callbacks;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

public interface PlayerBreakBlockCallback {
    Event<PlayerBreakBlockCallback> EVENT = new Event<>(
            listeners -> (player, pos, side) -> {
                for (PlayerBreakBlockCallback listener : listeners) {
                    listener.breakBlock(player, pos, side);
                }
            });

    void breakBlock(LocalPlayer player, BlockPos pos, Direction side);
}
