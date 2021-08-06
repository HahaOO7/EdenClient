package at.haha007.edenclient.callbacks;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.ActionResult;

public interface LeaveWorldCallback {
    Event<LeaveWorldCallback> EVENT = EventFactory.createArrayBacked(LeaveWorldCallback.class,
            listeners -> (world) -> {
                for (LeaveWorldCallback listener : listeners) {
                    ActionResult result = listener.leave(world);

                    if (result != ActionResult.PASS) {
                        return result;
                    }
                }
                return ActionResult.PASS;
            });

    ActionResult leave(ClientWorld world);
}
