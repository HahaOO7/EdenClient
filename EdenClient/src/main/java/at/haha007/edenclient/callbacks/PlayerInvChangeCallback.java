package at.haha007.edenclient.callbacks;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.world.entity.player.Inventory;

public interface PlayerInvChangeCallback {
    Event<PlayerInvChangeCallback> EVENT = EventFactory.createArrayBacked(PlayerInvChangeCallback.class,
            listeners -> (inv) -> {
                for (PlayerInvChangeCallback listener : listeners) {
                    listener.onInvChange(inv);
                }
            });

    void onInvChange(Inventory inv);
}
