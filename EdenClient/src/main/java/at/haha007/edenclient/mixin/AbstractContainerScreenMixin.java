package at.haha007.edenclient.mixin;

import at.haha007.edenclient.callbacks.ContainerCloseCallback;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(AbstractContainerScreen.class)
public class AbstractContainerScreenMixin<T extends AbstractContainerMenu> {
    @Shadow
    @Final
    protected T menu;

    @Inject(method = "onClose", at = @At("HEAD"))
    void onOnClose(CallbackInfo ci) {
        List<ItemStack> items = menu.getItems();
        if (items.size() > 27) items = items.subList(0, items.size() - 27);
        ContainerCloseCallback.EVENT.invoker().close(items);
    }
}
