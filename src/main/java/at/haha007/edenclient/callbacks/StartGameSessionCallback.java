package at.haha007.edenclient.callbacks;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.ActionResult;

public interface StartGameSessionCallback {
    Event<StartGameSessionCallback> EVENT = EventFactory.createArrayBacked(StartGameSessionCallback.class,
            listeners -> (player) -> {
                for (StartGameSessionCallback listener : listeners) {
                    ActionResult result = listener.join(player);

                    if (result != ActionResult.PASS) {
                        return result;
                    }
                }
                return ActionResult.PASS;
            });

    ActionResult join(ClientPlayerEntity player);
}
