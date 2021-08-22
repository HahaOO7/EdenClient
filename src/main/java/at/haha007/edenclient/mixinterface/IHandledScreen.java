package at.haha007.edenclient.mixinterface;

import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;

public interface IHandledScreen {
    void clickMouse(Slot slot, int slotId, int button, SlotActionType actionType);
}
