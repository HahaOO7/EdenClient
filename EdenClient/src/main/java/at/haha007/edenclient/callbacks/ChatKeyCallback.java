package at.haha007.edenclient.callbacks;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

import java.util.List;

public interface ChatKeyCallback {
    Event<ChatKeyCallback> EVENT = EventFactory.createArrayBacked(ChatKeyCallback.class,
            listeners -> (key, width, signWidth, posInHistory) -> {
                for (ChatKeyCallback listener : listeners) {
                    posInHistory = listener.getNewPosInHistory(key, width, signWidth, posInHistory);
                }
                return posInHistory;
            });

    int getNewPosInHistory(int key, List<String> recent, String prefix, int posInHistory);
}
