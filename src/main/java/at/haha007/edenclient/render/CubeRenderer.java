package at.haha007.edenclient.render;

import at.haha007.edenclient.callbacks.GameRenderCallback;
import at.haha007.edenclient.callbacks.JoinWorldCallback;
import at.haha007.edenclient.callbacks.LeaveWorldCallback;
import at.haha007.edenclient.callbacks.PlayerTickCallback;
import at.haha007.edenclient.utils.RenderUtils;
import at.haha007.edenclient.utils.singleton.Singleton;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.VertexBuffer;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.HashSet;
import java.util.Set;
import java.util.TreeMap;

@Singleton
public class CubeRenderer {
    private final TreeMap<Integer, Set<Box>> cubes = new TreeMap<>();
    private long tick = 0;
    private VertexBuffer box;

    private CubeRenderer() {
        JoinWorldCallback.EVENT.register(this::build);
        LeaveWorldCallback.EVENT.register(this::destroy);
        GameRenderCallback.EVENT.register(this::render);
        PlayerTickCallback.EVENT.register(this::tick);
    }

    private void tick(ClientPlayerEntity player) {
        if (cubes.isEmpty()) return;
        tick++;
        while (!cubes.isEmpty() && cubes.firstKey() < tick) {
            cubes.pollFirstEntry();
        }
    }

    private void destroy() {
        box.close();
        cubes.clear();
    }

    private void build() {
        box = new VertexBuffer();
        Box bb = new Box(-0.5, -0.5, -0.5, 0.5, 0.5, 0.5);
        RenderUtils.drawOutlinedBox(bb, box);
    }

    private void render(MatrixStack matrixStack, VertexConsumerProvider.Immediate vertexConsumerProvider, float v) {
        RenderSystem.setShader(GameRenderer::getPositionShader);
        RenderSystem.setShaderColor(1, 1, 1, 1);

        cubes.values().forEach(s -> s.forEach(box -> {
            matrixStack.push();
            Vec3d c = box.getCenter();
            matrixStack.translate(c.x, c.y, c.z);
            matrixStack.scale((float) (box.maxX - box.minX), (float) (box.maxY - box.minY), (float) (box.maxZ - box.minZ));
            this.box.setShader(matrixStack.peek().getModel(), RenderSystem.getProjectionMatrix(), RenderSystem.getShader());
            matrixStack.pop();
        }));
    }

    public void add(Box box, int ticks) {
        ticks += this.tick;
        Set<Box> set = cubes.computeIfAbsent(ticks, k -> new HashSet<>());
        set.add(box);
    }

    public void clear() {
        cubes.clear();
    }
}
