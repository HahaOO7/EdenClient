package at.haha007.edenclient.callbacks;

import net.minecraft.client.player.LocalPlayer;

public interface PlayerTickCallback {
    Event<PlayerTickCallback> EVENT = new Event<>(
            listeners -> player -> {
                for (PlayerTickCallback listener : listeners) {
                    listener.interact(player);
                }
            });

    void interact(LocalPlayer player);
}
