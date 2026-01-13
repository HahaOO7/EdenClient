package at.haha007.edenclient.mixin;

import at.haha007.edenclient.mixinterface.HandledScreenAccessor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(AbstractContainerScreen.class)
public abstract class HandledScreenMixin implements HandledScreenAccessor {
    @Shadow
    protected abstract void slotClicked(Slot slot, int slotId, int button, ClickType actionType);

    @Override
    public void edenClient$clickMouse(Slot slot, int slotId, int button, ClickType actionType) {
        slotClicked(slot, slotId, button, actionType);
    }
}
