package at.haha007.edenclient.mixin;

import at.haha007.edenclient.callbacks.PlayerInvChangeCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerInventory.class)
public class PlayerInventoryMixin {

    @Inject(
            at = @At("TAIL"),
            method = "setStack")
    void addItem(int slot, ItemStack stack, CallbackInfo ci) {
        //noinspection ConstantConditions
        PlayerInvChangeCallback.EVENT.invoker().onInvChange(MinecraftClient.getInstance().player.getInventory());
    }
}
