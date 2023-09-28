package at.haha007.edenclient.mixin;

import at.haha007.edenclient.callbacks.GameRenderCallback;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.opengl.GL11;
import org.objectweb.asm.Opcodes;
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
    private Camera mainCamera;

    @Shadow
    @Final
    private RenderBuffers renderBuffers;

    @Inject(method = "renderLevel",
            at = @At(value = "FIELD",
                    target = "Lnet/minecraft/client/renderer/GameRenderer;renderHand:Z",
                    opcode = Opcodes.GETFIELD,
                    ordinal = 0
            ))
    private void renderWorld(float tickDelta, long limitTime, PoseStack matrix, CallbackInfo ci) {
        matrix.pushPose();
        Vec3 cameraPos = mainCamera.getPosition();
        matrix.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);
        float[] color = RenderSystem.getShaderColor();
        RenderSystem.disableDepthTest();
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GameRenderCallback.EVENT.invoker().render(matrix, renderBuffers.bufferSource(), tickDelta);
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        RenderSystem.enableDepthTest();
        RenderSystem.setShaderColor(1,1,1,1);
        RenderSystem.setShaderColor(color[0], color[1], color[2], color[3]);
        matrix.popPose();
    }
}
