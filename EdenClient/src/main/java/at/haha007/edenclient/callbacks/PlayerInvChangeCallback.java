package at.haha007.edenclient.callbacks;

import net.minecraft.world.entity.player.Inventory;

public interface PlayerInvChangeCallback {
    Event<PlayerInvChangeCallback> EVENT = new Event<>(
            listeners -> (inv) -> {
                for (PlayerInvChangeCallback listener : listeners) {
                    listener.onInvChange(inv);
                }
            });

    void onInvChange(Inventory inv);
}
