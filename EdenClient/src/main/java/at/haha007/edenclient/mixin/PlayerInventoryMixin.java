package at.haha007.edenclient.mixin;

import at.haha007.edenclient.callbacks.PlayerInvChangeCallback;
import at.haha007.edenclient.utils.PlayerUtils;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Inventory.class)
public class PlayerInventoryMixin {

    @Inject(at = @At("TAIL"), method = "setItem")
    void addItem(int slot, ItemStack stack, CallbackInfo ci) {
        PlayerInvChangeCallback.EVENT.invoker().onInvChange(PlayerUtils.getPlayer().getInventory());
    }
}
