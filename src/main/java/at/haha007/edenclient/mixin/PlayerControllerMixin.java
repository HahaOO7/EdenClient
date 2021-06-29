package at.haha007.edenclient.mixin;

import at.haha007.edenclient.callbacks.PlayerAttackBlockCallback;
import at.haha007.edenclient.callbacks.PlayerInteractBlockEvent;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientPlayerInteractionManager.class)
public class PlayerControllerMixin {

    @Inject(at = @At("HEAD"),
            method = "interactBlock",
            cancellable = true)
    void interactBlock(ClientPlayerEntity player, ClientWorld world, Hand hand, BlockHitResult hitResult, CallbackInfoReturnable<ActionResult> ci) {
        ActionResult result = PlayerInteractBlockEvent.EVENT.invoker().interact(player, world, hand, hitResult);
        if (result == ActionResult.FAIL) {
            ci.setReturnValue(ActionResult.FAIL);
            ci.cancel();
        }

    }

    @Inject(method = "attackBlock", at = @At("HEAD"), cancellable = true)
    private void onAttackBlock(BlockPos pos, Direction side, CallbackInfoReturnable<Boolean> cir) {
        ActionResult result = PlayerAttackBlockCallback.EVENT.invoker().interact(MinecraftClient.getInstance().player, pos, side);
        if (result == ActionResult.FAIL) cir.cancel();
    }

}
