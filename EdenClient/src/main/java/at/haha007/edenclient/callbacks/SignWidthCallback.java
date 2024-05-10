package at.haha007.edenclient.callbacks;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

public interface SignWidthCallback {
    Event<SignWidthCallback> EVENT = EventFactory.createArrayBacked(SignWidthCallback.class,
            listeners -> (width, signWidth, edgeReached) -> {
                for (SignWidthCallback listener : listeners) {
                    edgeReached = listener.canContinueWriting(width, signWidth, edgeReached);
                }
                return edgeReached;
            });

    boolean canContinueWriting(int textWidth, int signWidth, boolean edgeReached);
}
