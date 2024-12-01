package at.haha007.edenclient.render;

import at.haha007.edenclient.annotations.Mod;
import at.haha007.edenclient.callbacks.GameRenderCallback;
import at.haha007.edenclient.callbacks.JoinWorldCallback;
import at.haha007.edenclient.callbacks.LeaveWorldCallback;
import at.haha007.edenclient.callbacks.PlayerTickCallback;
import at.haha007.edenclient.utils.RenderUtils;
import com.mojang.blaze3d.buffers.BufferUsage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexBuffer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.CoreShaders;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;
import java.util.TreeMap;

@Mod
public class CubeRenderer {
    private final TreeMap<Integer, Set<AABB>> cubes = new TreeMap<>();
    private long tick = 0;
    private VertexBuffer box;

    public CubeRenderer() {
        JoinWorldCallback.EVENT.register(this::build, getClass());
        LeaveWorldCallback.EVENT.register(this::destroy, getClass());
        GameRenderCallback.EVENT.register(this::render, getClass());
        PlayerTickCallback.EVENT.register(this::tick, getClass());
    }

    private void tick(LocalPlayer player) {
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
        box = new VertexBuffer(BufferUsage.STATIC_WRITE);
        AABB bb = new AABB(-0.5, -0.5, -0.5, 0.5, 0.5, 0.5);
        RenderUtils.drawOutlinedBox(bb, box);
    }

    private void render(PoseStack matrixStack, MultiBufferSource.BufferSource vertexConsumerProvider, float v) {
        RenderSystem.setShader(Minecraft.getInstance().getShaderManager().getProgram(CoreShaders.POSITION));
        RenderSystem.setShaderColor(1, 1, 1, 1);

        cubes.values().forEach(s -> s.forEach(boundingBox -> {
            matrixStack.pushPose();
            Vec3 c = boundingBox.getCenter();
            matrixStack.translate(c.x, c.y, c.z);
            matrixStack.scale((float) (boundingBox.maxX - boundingBox.minX), (float) (boundingBox.maxY - boundingBox.minY), (float) (boundingBox.maxZ - boundingBox.minZ));
            this.box.bind();
            this.box.drawWithShader(matrixStack.last().pose(), RenderSystem.getProjectionMatrix(), RenderSystem.getShader());
            VertexBuffer.unbind();
            matrixStack.popPose();
        }));
    }

    public void add(AABB box, int ticks) {
        ticks += (int) this.tick;
        Set<AABB> set = cubes.computeIfAbsent(ticks, k -> new HashSet<>());
        set.add(box);
    }

    public void clear() {
        cubes.clear();
    }
}
