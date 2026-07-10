package at.haha007.edenclient.mixin;

import at.haha007.edenclient.callbacks.ShouldDoFakeSneakCallback;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public class PlayerMixin {
    @Inject(at = @At("HEAD"), method = "isStayingOnGroundSurface", cancellable = true)
    void isStayingOnGroundSurface(CallbackInfoReturnable<Boolean> cir) {
        if (ShouldDoFakeSneakCallback.EVENT.invoker().shouldDoFakeSneak()) {
            cir.setReturnValue(true);
        }
    }

}
