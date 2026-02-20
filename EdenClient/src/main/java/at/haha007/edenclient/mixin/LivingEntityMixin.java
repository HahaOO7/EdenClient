package at.haha007.edenclient.mixin;

import at.haha007.edenclient.callbacks.StepUpHeightCallback;
import at.haha007.edenclient.utils.PlayerUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @Inject(at = @At("HEAD"), method = "maxUpStep", cancellable = true)
    public void maxUpStep(CallbackInfoReturnable<Float> cir) {
        if(Minecraft.getInstance().player == null) return;
        if ((Object) this != PlayerUtils.getPlayer()) {
            return;
        }
        Optional<Float> stepUpHeight = StepUpHeightCallback.EVENT.invoker().getStepUpHeight();
        stepUpHeight.ifPresent(cir::setReturnValue);
    }

}
