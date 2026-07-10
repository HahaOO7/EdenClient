package at.haha007.edenclient.mixinterface;

import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;

public interface HandledScreenAccessor {
    void edenClient$clickMouse(Slot slot, int slotId, int button, ContainerInput actionType);
}
