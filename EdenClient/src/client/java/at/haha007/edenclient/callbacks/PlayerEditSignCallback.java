package at.haha007.edenclient.callbacks;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.entity.SignBlockEntity;

public interface PlayerEditSignCallback {
    Event<PlayerEditSignCallback> EVENT = new Event<>(
            listeners -> (player, sign, front) -> {
                InteractionResult result = InteractionResult.PASS;
                for (PlayerEditSignCallback listener : listeners) {
                    InteractionResult r = listener.interact(player, sign, front);
                    if (r != InteractionResult.PASS) {
                        result = r;
                    }
                }
                return result;
            });

    InteractionResult interact(LocalPlayer player, SignBlockEntity sign, boolean front);
}
