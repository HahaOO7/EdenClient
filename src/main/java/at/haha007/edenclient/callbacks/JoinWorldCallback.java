package at.haha007.edenclient.callbacks;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.client.world.ClientWorld;

public interface JoinWorldCallback {
    Event<JoinWorldCallback> EVENT = EventFactory.createArrayBacked(JoinWorldCallback.class,
            listeners -> (world) -> {
                for (JoinWorldCallback listener : listeners) {
                    listener.join(world);
                }
            });

    void join(ClientWorld world);
}
