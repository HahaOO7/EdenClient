package at.haha007.edenclient.callbacks;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

public interface ConfigLoadedCallback {
    Event<ConfigLoadedCallback> EVENT = EventFactory.createArrayBacked(ConfigLoadedCallback.class,
            listeners -> () -> {
                for (ConfigLoadedCallback listener : listeners) {
                    listener.configLoaded();
                }
            });

    void configLoaded();
}
