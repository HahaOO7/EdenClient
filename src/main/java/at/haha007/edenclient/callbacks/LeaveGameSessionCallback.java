package at.haha007.edenclient.callbacks;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.util.ActionResult;

public interface LeaveGameSessionCallback {
    Event<LeaveGameSessionCallback> EVENT = EventFactory.createArrayBacked(LeaveGameSessionCallback.class,
            listeners -> () -> {
                for (LeaveGameSessionCallback listener : listeners) {
                    ActionResult result = listener.leave();

                    if (result != ActionResult.PASS) {
                        return result;
                    }
                }
                return ActionResult.PASS;
            });

    ActionResult leave();
}
