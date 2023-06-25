package at.haha007.edenclient.utils;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public enum RenderUtils {
    ;

    public static Vec3 getCameraPos() {
        return Minecraft.getInstance().getBlockEntityRenderDispatcher().camera.getPosition();
    }

    public static void drawOutlinedBox(AABB bb, VertexBuffer vertexBuffer) {
        BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();

        bufferBuilder.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION);

        bufferBuilder.vertex(bb.minX, bb.minY, bb.minZ).endVertex();
        bufferBuilder.vertex(bb.maxX, bb.minY, bb.minZ).endVertex();

        bufferBuilder.vertex(bb.maxX, bb.minY, bb.minZ).endVertex();
        bufferBuilder.vertex(bb.maxX, bb.minY, bb.maxZ).endVertex();

        bufferBuilder.vertex(bb.maxX, bb.minY, bb.maxZ).endVertex();
        bufferBuilder.vertex(bb.minX, bb.minY, bb.maxZ).endVertex();

        bufferBuilder.vertex(bb.minX, bb.minY, bb.maxZ).endVertex();
        bufferBuilder.vertex(bb.minX, bb.minY, bb.minZ).endVertex();

        bufferBuilder.vertex(bb.minX, bb.minY, bb.minZ).endVertex();
        bufferBuilder.vertex(bb.minX, bb.maxY, bb.minZ).endVertex();

        bufferBuilder.vertex(bb.maxX, bb.minY, bb.minZ).endVertex();
        bufferBuilder.vertex(bb.maxX, bb.maxY, bb.minZ).endVertex();

        bufferBuilder.vertex(bb.maxX, bb.minY, bb.maxZ).endVertex();
        bufferBuilder.vertex(bb.maxX, bb.maxY, bb.maxZ).endVertex();

        bufferBuilder.vertex(bb.minX, bb.minY, bb.maxZ).endVertex();
        bufferBuilder.vertex(bb.minX, bb.maxY, bb.maxZ).endVertex();

        bufferBuilder.vertex(bb.minX, bb.maxY, bb.minZ).endVertex();
        bufferBuilder.vertex(bb.maxX, bb.maxY, bb.minZ).endVertex();

        bufferBuilder.vertex(bb.maxX, bb.maxY, bb.minZ).endVertex();
        bufferBuilder.vertex(bb.maxX, bb.maxY, bb.maxZ).endVertex();

        bufferBuilder.vertex(bb.maxX, bb.maxY, bb.maxZ).endVertex();
        bufferBuilder.vertex(bb.minX, bb.maxY, bb.maxZ).endVertex();

        bufferBuilder.vertex(bb.minX, bb.maxY, bb.maxZ).endVertex();
        bufferBuilder.vertex(bb.minX, bb.maxY, bb.minZ).endVertex();
        vertexBuffer.bind();
        vertexBuffer.upload(bufferBuilder.end());
        VertexBuffer.unbind();
    }

    public static void drawSolidBox(AABB bb, VertexBuffer vertexBuffer) {
        BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
        bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);

        bufferBuilder.vertex(bb.minX, bb.minY, bb.minZ).endVertex();
        bufferBuilder.vertex(bb.maxX, bb.minY, bb.minZ).endVertex();
        bufferBuilder.vertex(bb.maxX, bb.minY, bb.maxZ).endVertex();
        bufferBuilder.vertex(bb.minX, bb.minY, bb.maxZ).endVertex();

        bufferBuilder.vertex(bb.minX, bb.maxY, bb.minZ).endVertex();
        bufferBuilder.vertex(bb.minX, bb.maxY, bb.maxZ).endVertex();
        bufferBuilder.vertex(bb.maxX, bb.maxY, bb.maxZ).endVertex();
        bufferBuilder.vertex(bb.maxX, bb.maxY, bb.minZ).endVertex();

        bufferBuilder.vertex(bb.minX, bb.minY, bb.minZ).endVertex();
        bufferBuilder.vertex(bb.minX, bb.maxY, bb.minZ).endVertex();
        bufferBuilder.vertex(bb.maxX, bb.maxY, bb.minZ).endVertex();
        bufferBuilder.vertex(bb.maxX, bb.minY, bb.minZ).endVertex();

        bufferBuilder.vertex(bb.maxX, bb.minY, bb.minZ).endVertex();
        bufferBuilder.vertex(bb.maxX, bb.maxY, bb.minZ).endVertex();
        bufferBuilder.vertex(bb.maxX, bb.maxY, bb.maxZ).endVertex();
        bufferBuilder.vertex(bb.maxX, bb.minY, bb.maxZ).endVertex();

        bufferBuilder.vertex(bb.minX, bb.minY, bb.maxZ).endVertex();
        bufferBuilder.vertex(bb.maxX, bb.minY, bb.maxZ).endVertex();
        bufferBuilder.vertex(bb.maxX, bb.maxY, bb.maxZ).endVertex();
        bufferBuilder.vertex(bb.minX, bb.maxY, bb.maxZ).endVertex();

        bufferBuilder.vertex(bb.minX, bb.minY, bb.minZ).endVertex();
        bufferBuilder.vertex(bb.minX, bb.minY, bb.maxZ).endVertex();
        bufferBuilder.vertex(bb.minX, bb.maxY, bb.maxZ).endVertex();
        bufferBuilder.vertex(bb.minX, bb.maxY, bb.minZ).endVertex();

        vertexBuffer.bind();
        vertexBuffer.upload(bufferBuilder.end());
        VertexBuffer.unbind();
    }
}
