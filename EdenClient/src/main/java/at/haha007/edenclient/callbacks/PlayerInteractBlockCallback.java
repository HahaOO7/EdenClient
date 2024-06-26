package at.haha007.edenclient.callbacks;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.BlockHitResult;

public interface PlayerInteractBlockCallback {
    Event<PlayerInteractBlockCallback> EVENT = new Event<>(
            listeners -> (player, world, hand, hitResult) -> {
                InteractionResult result = InteractionResult.PASS;
                for (PlayerInteractBlockCallback listener : listeners) {
                    InteractionResult r = listener.interact(player, world, hand, hitResult);
                    if (r != InteractionResult.PASS) {
                        result = r;
                    }
                }
                return result;
            });

    InteractionResult interact(LocalPlayer player, ClientLevel world, InteractionHand hand, BlockHitResult hitResult);
}
