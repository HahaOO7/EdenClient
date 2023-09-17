package at.haha007.edenclient.mixin;

import at.haha007.edenclient.callbacks.WorldRenderCallback;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public class WorldRendererMixin {

    @Shadow
    @Final
    private RenderBuffers renderBuffers;

    @Inject(method = "renderLevel", at = @At("HEAD"))
    private void renderWorld(PoseStack matrix, float tickDelta, long limitTime, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightTexture lightmapTextureManager, Matrix4f positionMatrix, CallbackInfo ci) {
        matrix.pushPose();
        Vec3 cameraPos = camera.getPosition();
        matrix.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);
        float[] color = RenderSystem.getShaderColor();

        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        WorldRenderCallback.EVENT.invoker().render(matrix, renderBuffers.bufferSource(), tickDelta);
        GL11.glDisable(GL11.GL_LINE_SMOOTH);

        RenderSystem.enableDepthTest();
        RenderSystem.setShaderColor(1,1,1,1);
        RenderSystem.setShaderColor(color[0], color[1], color[2], color[3]);
        matrix.popPose();
    }
}
