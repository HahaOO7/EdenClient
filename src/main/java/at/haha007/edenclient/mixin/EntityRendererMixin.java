package at.haha007.edenclient.mixin;

import at.haha007.edenclient.callbacks.EntityRenderCallback;
import at.haha007.edenclient.utils.RenderUtils;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
public class EntityRendererMixin<T extends Entity> {

    private final double extraSize = 0;
    private final Box defaultBox = Box.from(new Vec3d(0, 0, 0)).expand(.25);

    @Inject(at = @At("HEAD"),
            method = "render")
    void render(T entity, float yaw, float tickDelta, MatrixStack matrixStack, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        EntityRenderCallback.EVENT.invoker().onRender(entity, tickDelta, matrixStack);
    }
}
