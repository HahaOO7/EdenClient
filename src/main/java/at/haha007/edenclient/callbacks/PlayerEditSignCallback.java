package at.haha007.edenclient.callbacks;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.ActionResult;

public interface PlayerEditSignCallback {
    Event<PlayerEditSignCallback> EVENT = EventFactory.createArrayBacked(PlayerEditSignCallback.class,
            listeners -> (player, sign) -> {
                ActionResult result = ActionResult.PASS;
                for (PlayerEditSignCallback listener : listeners) {
                    ActionResult r = listener.interact(player, sign);
                    if (r != ActionResult.PASS) {
                        result = r;
                    }
                }
                return result;
            });

    ActionResult interact(ClientPlayerEntity player, SignBlockEntity sign);
}
