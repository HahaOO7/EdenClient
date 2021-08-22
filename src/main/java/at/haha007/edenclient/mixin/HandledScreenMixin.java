package at.haha007.edenclient.mixin;

import at.haha007.edenclient.mixinterface.IHandledScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(HandledScreen.class)
public abstract class HandledScreenMixin implements IHandledScreen {
    @Shadow
    protected abstract void onMouseClick(Slot slot, int slotId, int button, SlotActionType actionType);

    @Override
    public void clickMouse(Slot slot, int slotId, int button, SlotActionType actionType) {
        onMouseClick(slot, slotId, button, actionType);
    }
}
