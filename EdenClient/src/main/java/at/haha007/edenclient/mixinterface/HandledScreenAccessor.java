package at.haha007.edenclient.mixinterface;

import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;

public interface HandledScreenAccessor {
    void edenClient$clickMouse(Slot slot, int slotId, int button, ClickType actionType);
}
