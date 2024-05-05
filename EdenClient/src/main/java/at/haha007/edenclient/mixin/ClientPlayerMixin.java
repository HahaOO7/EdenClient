package at.haha007.edenclient.mixin;

import at.haha007.edenclient.callbacks.PlayerEditSignCallback;
import at.haha007.edenclient.callbacks.PlayerTickCallback;
import at.haha007.edenclient.utils.PlayerUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(LocalPlayer.class)
public abstract class ClientPlayerMixin {

    @Shadow
    @Final
    public ClientPacketListener connection;

    @Shadow
    @Final
    protected Minecraft minecraft;

    @Inject(at = @At("HEAD"), method = "openTextEdit", cancellable = true)
    private void onEditSign(SignBlockEntity sign, boolean front, CallbackInfo info) {
        InteractionResult result = PlayerEditSignCallback.EVENT.invoker().interact(PlayerUtils.getPlayer(), sign, front);
        if (result == InteractionResult.FAIL) info.cancel();
    }

    @Inject(at = @At("HEAD"), method = "tick")
    void tickMovement(CallbackInfo ci) {
        PlayerTickCallback.EVENT.invoker().interact(PlayerUtils.getPlayer());
    }

}
