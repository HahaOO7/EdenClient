package at.haha007.edenclient.render;

import at.haha007.edenclient.annotations.Mod;
import at.haha007.edenclient.callbacks.GameRenderCallback;
import at.haha007.edenclient.callbacks.LeaveWorldCallback;
import at.haha007.edenclient.callbacks.PlayerTickCallback;
import at.haha007.edenclient.utils.PlayerUtils;
import at.haha007.edenclient.utils.RenderUtils;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.HashSet;
import java.util.Set;
import java.util.TreeMap;

@Mod
public class TracerRenderer {
    private final TreeMap<Integer, Set<Vec3>> tracers = new TreeMap<>();
    private long tick = 0;

    public TracerRenderer() {
        GameRenderCallback.EVENT.register(this::render, getClass());
        PlayerTickCallback.EVENT.register(this::tick, getClass());
        LeaveWorldCallback.EVENT.register(tracers::clear, getClass());
    }

    private void tick(LocalPlayer player) {
        if (tracers.isEmpty()) return;
        tick++;
        while (!tracers.isEmpty() && tracers.firstKey() < tick) {
            tracers.pollFirstEntry();
        }
    }

    private void render(PoseStack matrixStack, MultiBufferSource.BufferSource vertexConsumerProvider, float delta) {
        if (tracers.isEmpty()) return;
        RenderSystem.setShader(GameRenderer::getPositionShader);
        RenderSystem.disableDepthTest();
        Matrix4f matrix = matrixStack.last().pose();
        Vec3 start = RenderUtils.getCameraPos().add(PlayerUtils.getClientLookVec());

        BufferBuilder bb = Tesselator.getInstance().begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION);
        tracers.values().forEach(s -> s.forEach(target -> {
            bb.addVertex(matrix, (float) target.x, (float) target.y, (float) target.z);
            bb.addVertex(matrix, (float) start.x, (float) start.y, (float) start.z);
        }));
        RenderSystem.setShaderColor(1, 1, 1, 1);
        BufferUploader.drawWithShader(bb.buildOrThrow());
    }

    public void add(Vec3 target, int ticks) {
        ticks += (int) this.tick;
        Set<Vec3> set = tracers.computeIfAbsent(ticks, k -> new HashSet<>());
        set.add(target);
    }

    public void clear() {
        tracers.clear();
    }
}
