package at.haha007.edenclient.callbacks;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.ActionResult;

public interface LeaveGameSessionCallback {
    Event<LeaveGameSessionCallback> EVENT = EventFactory.createArrayBacked(LeaveGameSessionCallback.class,
            listeners -> (player) -> {
                for (LeaveGameSessionCallback listener : listeners) {
                    ActionResult result = listener.leave(player);

                    if (result != ActionResult.PASS) {
                        return result;
                    }
                }
                return ActionResult.PASS;
            });

    ActionResult leave(ClientPlayerEntity player);
}
