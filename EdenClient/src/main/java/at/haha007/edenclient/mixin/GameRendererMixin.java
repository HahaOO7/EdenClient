package at.haha007.edenclient.mixin;

import at.haha007.edenclient.callbacks.GameRenderCallback;
import at.haha007.edenclient.utils.PlayerUtils;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.lwjgl.opengl.GL11;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {

    @Shadow
    @Final
    private Camera mainCamera;

    @Shadow
    @Final
    private RenderBuffers renderBuffers;

    @Shadow
    public abstract Matrix4f getProjectionMatrix(double d);


    @Inject(at = @At(value = "FIELD",
            target = "Lnet/minecraft/client/renderer/GameRenderer;renderHand:Z",
            opcode = Opcodes.GETFIELD,
            ordinal = 0),
            method = "renderLevel")
    private void renderWorld(DeltaTracker deltaTracker, CallbackInfo ci, @Local(ordinal = 1) Matrix4f matrix4f2) {

        Vec3 position = mainCamera.getPosition();

        PoseStack matrix = new PoseStack();
        matrix.mulPose(matrix4f2);
        matrix.translate(-position.x,  -position.y, -position.z);
        matrix.pushPose();

        float[] color = RenderSystem.getShaderColor();
        RenderSystem.disableDepthTest();

        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GameRenderCallback.EVENT.invoker().render(matrix, renderBuffers.bufferSource(), deltaTracker.getGameTimeDeltaPartialTick(true));
        GL11.glDisable(GL11.GL_LINE_SMOOTH);

        RenderSystem.enableDepthTest();
        RenderSystem.setShaderColor(1, 1, 1, 1);
        RenderSystem.setShaderColor(color[0], color[1], color[2], color[3]);
    }
}
