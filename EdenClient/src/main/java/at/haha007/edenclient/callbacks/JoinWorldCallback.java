package at.haha007.edenclient.callbacks;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

public interface JoinWorldCallback {
    Event<JoinWorldCallback> EVENT = EventFactory.createArrayBacked(JoinWorldCallback.class,
            listeners -> () -> {
                for (JoinWorldCallback listener : listeners) {
                    listener.join();
                }
            });

    void join();
}
