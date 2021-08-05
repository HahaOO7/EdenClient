package at.haha007.edenclient.callbacks;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.util.ActionResult;

public interface StartGameSessionCallback {
    Event<StartGameSessionCallback> EVENT = EventFactory.createArrayBacked(StartGameSessionCallback.class,
            listeners -> () -> {
                for (StartGameSessionCallback listener : listeners) {
                    ActionResult result = listener.join();

                    if (result != ActionResult.PASS) {
                        return result;
                    }
                }
                return ActionResult.PASS;
            });

    ActionResult join();
}
