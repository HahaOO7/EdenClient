package at.haha007.edenclient.mixin;

import at.haha007.edenclient.callbacks.ItemRenderCallback;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.ItemEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntityRenderer.class)
public class ItemRendererMixin {


    @Inject(at = @At("TAIL"),
            method = "render")
    void onItemRender(ItemEntity itemEntity,
                      float yaw,
                      float tickDelta,
                      MatrixStack matrixStack,
                      VertexConsumerProvider vertexConsumerProvider,
                      int light,
                      CallbackInfo ci) {
        ItemRenderCallback.EVENT.invoker().renderItem(itemEntity, yaw, tickDelta, light, matrixStack);
    }
}
