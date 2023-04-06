package at.haha007.edenclient.mixin;

import at.haha007.edenclient.callbacks.GameRenderCallback;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.BufferBuilderStorage;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class GameRendererMixin {

    @Shadow
    @Final
    private Camera camera;

    @Shadow
    @Final
    private BufferBuilderStorage buffers;

    @Inject(method = "renderWorld", at = @At(value = "INVOKE_STRING", target = "Lnet/minecraft/util/profiler/Profiler;swap(Ljava/lang/String;)V", args = {"ldc=hand"}))
    private void renderWorld(float tickDelta, long limitTime, MatrixStack matrix, CallbackInfo ci) {
        matrix.push();
        Vec3d cameraPos = camera.getPos();
        matrix.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);
        float[] color = RenderSystem.getShaderColor();
        RenderSystem.disableDepthTest();
        GameRenderCallback.EVENT.invoker().render(matrix, buffers.getEntityVertexConsumers(), tickDelta);
        RenderSystem.enableDepthTest();
        RenderSystem.setShaderColor(1,1,1,1);
        matrix.pop();
    }
}
