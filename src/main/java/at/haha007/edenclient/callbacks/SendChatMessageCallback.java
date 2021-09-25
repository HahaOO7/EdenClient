package at.haha007.edenclient.callbacks;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

public interface SendChatMessageCallback {
    Event<SendChatMessageCallback> EVENT = EventFactory.createArrayBacked(SendChatMessageCallback.class,
            listeners -> (msg) -> {
                for (SendChatMessageCallback listener : listeners) {
                    listener.sendMessage(msg);
                }
            });

    void sendMessage(String message);
}
