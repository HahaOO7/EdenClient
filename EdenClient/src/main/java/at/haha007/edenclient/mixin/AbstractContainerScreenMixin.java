package at.haha007.edenclient.mixin;

import at.haha007.edenclient.callbacks.ContainerCloseCallback;
import at.haha007.edenclient.callbacks.InventoryKeyCallback;
import at.haha007.edenclient.mixinterface.HoveredSlotAccessor;
import net.minecraft.client.GuiMessage;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(AbstractContainerScreen.class)
public class AbstractContainerScreenMixin<T extends AbstractContainerMenu> implements HoveredSlotAccessor {
    @Shadow
    @Final
    protected T menu;

    @Shadow
    @Nullable
    protected Slot hoveredSlot;

    @Inject(method = "onClose", at = @At("HEAD"))
    void onOnClose(CallbackInfo ci) {
        List<ItemStack> items = menu.getItems();
        if (items.size() > 27) items = items.subList(0, items.size() - 36);
        ContainerCloseCallback.EVENT.invoker().close(items);
    }

    @Inject(at = @At("HEAD"), method = "keyPressed")
    private void onKeyPressed(KeyEvent keyEvent, CallbackInfoReturnable<Boolean> cir) {
        InventoryKeyCallback.EVENT.invoker().onKeyEvent(keyEvent, (AbstractContainerScreen<?>) (Object) this);
    }

    @Override
    public Slot edenClient$getHoveredSlot() {
        return this.hoveredSlot;
    }
}
