package at.haha007.edenclient.render;

import at.haha007.edenclient.callbacks.GameRenderCallback;
import at.haha007.edenclient.callbacks.LeaveWorldCallback;
import at.haha007.edenclient.callbacks.PlayerTickCallback;
import at.haha007.edenclient.utils.PlayerUtils;
import at.haha007.edenclient.utils.RenderUtils;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.util.HashSet;
import java.util.Set;
import java.util.TreeMap;

public class TracerRenderer {
    private final TreeMap<Integer, Set<Vec3d>> tracers = new TreeMap<>();
    private long tick = 0;

    public TracerRenderer() {
        GameRenderCallback.EVENT.register(this::render);
        PlayerTickCallback.EVENT.register(this::tick);
        LeaveWorldCallback.EVENT.register(tracers::clear);
    }

    private void tick(ClientPlayerEntity player) {
        if (tracers.isEmpty()) return;
        tick++;
        while (!tracers.isEmpty() && tracers.firstKey() < tick) {
            tracers.pollFirstEntry();
        }
    }

    private void render(MatrixStack matrixStack, VertexConsumerProvider.Immediate vertexConsumerProvider, float delta) {
        RenderSystem.setShader(GameRenderer::getPositionProgram);
        RenderSystem.disableDepthTest();
        Matrix4f matrix = matrixStack.peek().getPositionMatrix();
        Vec3d start = RenderUtils.getCameraPos().add(PlayerUtils.getClientLookVec());

        BufferBuilder bb = Tessellator.getInstance().getBuffer();
        bb.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION);
        tracers.values().forEach(s -> s.forEach(target -> {
            bb.vertex(matrix, (float) target.x, (float) target.y, (float) target.z).next();
            bb.vertex(matrix, (float) start.x, (float) start.y, (float) start.z).next();
        }));
        RenderSystem.setShaderColor(1, 1, 1, 1);
        BufferRenderer.drawWithGlobalProgram(bb.end());
    }

    public void add(Vec3d target, int ticks) {
        ticks += this.tick;
        Set<Vec3d> set = tracers.computeIfAbsent(ticks, k -> new HashSet<>());
        set.add(target);
    }

    public void clear() {
        tracers.clear();
    }
}
