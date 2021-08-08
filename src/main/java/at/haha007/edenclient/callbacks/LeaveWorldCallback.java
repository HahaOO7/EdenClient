package at.haha007.edenclient.callbacks;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.client.world.ClientWorld;

public interface LeaveWorldCallback {
    Event<LeaveWorldCallback> EVENT = EventFactory.createArrayBacked(LeaveWorldCallback.class,
            listeners -> (world) -> {
                for (LeaveWorldCallback listener : listeners) {
                    listener.leave(world);
                }
            });

    void leave(ClientWorld world);
}
