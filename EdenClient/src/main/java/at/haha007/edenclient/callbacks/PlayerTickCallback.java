package at.haha007.edenclient.callbacks;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.client.player.LocalPlayer;

public interface PlayerTickCallback {
    Event<PlayerTickCallback> EVENT = EventFactory.createArrayBacked(PlayerTickCallback.class,
            listeners -> player -> {
                for (PlayerTickCallback listener : listeners) {
                    listener.interact(player);
                }
            });

    void interact(LocalPlayer player);
}
