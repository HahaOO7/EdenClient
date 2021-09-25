package at.haha007.edenclient.mixin;

import at.haha007.edenclient.callbacks.WorldRenderCallback;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public class WorldRendererMixin {

    @Shadow
    @Final
    private BufferBuilderStorage bufferBuilders;

    @Inject(method = "render", at = @At("HEAD"))
    private void renderWorld(MatrixStack matrix, float tickDelta, long limitTime, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightmapTextureManager lightmapTextureManager, Matrix4f matrix4f, CallbackInfo ci) {
        if (MinecraftClient.getInstance().getBlockEntityRenderDispatcher().camera == null)
            MinecraftClient.getInstance().getBlockEntityRenderDispatcher().camera = camera;
        matrix.push();
        Vec3d cameraPos = camera.getPos();
        matrix.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);

        RenderSystem.disableDepthTest();
        WorldRenderCallback.EVENT.invoker().render(matrix, bufferBuilders.getEntityVertexConsumers(), tickDelta);
        RenderSystem.enableDepthTest();

        matrix.pop();
    }
}
