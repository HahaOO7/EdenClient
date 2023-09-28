package at.haha007.edenclient.callbacks;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

public interface LeaveWorldCallback {
    Event<LeaveWorldCallback> EVENT = EventFactory.createArrayBacked(LeaveWorldCallback.class,
            listeners -> () -> {
                for (LeaveWorldCallback listener : listeners) {
                    listener.leave();
                }
            });

    void leave();
}
