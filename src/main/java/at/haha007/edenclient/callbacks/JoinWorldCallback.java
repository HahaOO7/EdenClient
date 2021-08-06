package at.haha007.edenclient.callbacks;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.ActionResult;

public interface JoinWorldCallback {
    Event<JoinWorldCallback> EVENT = EventFactory.createArrayBacked(JoinWorldCallback.class,
            listeners -> (world) -> {
                for (JoinWorldCallback listener : listeners) {
                    ActionResult result = listener.join(world);

                    if (result != ActionResult.PASS) {
                        return result;
                    }
                }
                return ActionResult.PASS;
            });

    ActionResult join(ClientWorld world);
}
