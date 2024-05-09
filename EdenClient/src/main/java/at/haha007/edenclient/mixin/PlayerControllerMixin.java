package at.haha007.edenclient.mixin;

import at.haha007.edenclient.callbacks.PlayerAttackBlockCallback;
import at.haha007.edenclient.callbacks.PlayerInteractBlockCallback;
import at.haha007.edenclient.utils.PlayerUtils;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiPlayerGameMode.class)
public class PlayerControllerMixin {

    @Inject(at = @At("HEAD"),
            method = "performUseItemOn",
            cancellable = true)
    void interactBlock(LocalPlayer player, InteractionHand hand, BlockHitResult hitResult, CallbackInfoReturnable<InteractionResult> ci) {
        InteractionResult result = PlayerInteractBlockCallback.EVENT.invoker().interact(player, player.clientLevel, hand, hitResult);
        if (result == InteractionResult.FAIL) {
            ci.setReturnValue(InteractionResult.FAIL);
            ci.cancel();
        }
    }

    @Inject(method = "startDestroyBlock", at = @At("HEAD"), cancellable = true)
    private void onAttackBlock(BlockPos pos, Direction side, CallbackInfoReturnable<Boolean> cir) {
        InteractionResult result = PlayerAttackBlockCallback.EVENT.invoker().interact(PlayerUtils.getPlayer(), pos, side);
        if (result == InteractionResult.FAIL) cir.setReturnValue(false);
    }

    @Inject(method = "continueDestroyBlock", at = @At("HEAD"), cancellable = true)
    private void onContinueDestroyBlock(BlockPos pos, Direction side, CallbackInfoReturnable<Boolean> cir) {
        InteractionResult result = PlayerAttackBlockCallback.EVENT.invoker().interact(PlayerUtils.getPlayer(), pos, side);
        if (result == InteractionResult.FAIL) cir.setReturnValue(false);
    }

    @Inject(method = "destroyBlock", at = @At("HEAD"), cancellable = true)
    private void onDestroyBlock(BlockPos blockPos, CallbackInfoReturnable<Boolean> cir) {
        InteractionResult result = PlayerAttackBlockCallback.EVENT.invoker().interact(PlayerUtils.getPlayer(), blockPos, Direction.UP);
        if (result == InteractionResult.FAIL) cir.setReturnValue(false);
    }

}
