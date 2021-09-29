package at.haha007.edenclient.utils;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.VertexBuffer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

public enum RenderUtils {
    ;

    public static Vec3d getCameraPos() {
        return MinecraftClient.getInstance().getBlockEntityRenderDispatcher().camera.getPos();
    }

    public static void drawOutlinedBox(Box bb, VertexBuffer vertexBuffer) {
        BufferBuilder bufferBuilder = Tessellator.getInstance().getBuffer();

        bufferBuilder.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION);

        bufferBuilder.vertex(bb.minX, bb.minY, bb.minZ).next();
        bufferBuilder.vertex(bb.maxX, bb.minY, bb.minZ).next();

        bufferBuilder.vertex(bb.maxX, bb.minY, bb.minZ).next();
        bufferBuilder.vertex(bb.maxX, bb.minY, bb.maxZ).next();

        bufferBuilder.vertex(bb.maxX, bb.minY, bb.maxZ).next();
        bufferBuilder.vertex(bb.minX, bb.minY, bb.maxZ).next();

        bufferBuilder.vertex(bb.minX, bb.minY, bb.maxZ).next();
        bufferBuilder.vertex(bb.minX, bb.minY, bb.minZ).next();

        bufferBuilder.vertex(bb.minX, bb.minY, bb.minZ).next();
        bufferBuilder.vertex(bb.minX, bb.maxY, bb.minZ).next();

        bufferBuilder.vertex(bb.maxX, bb.minY, bb.minZ).next();
        bufferBuilder.vertex(bb.maxX, bb.maxY, bb.minZ).next();

        bufferBuilder.vertex(bb.maxX, bb.minY, bb.maxZ).next();
        bufferBuilder.vertex(bb.maxX, bb.maxY, bb.maxZ).next();

        bufferBuilder.vertex(bb.minX, bb.minY, bb.maxZ).next();
        bufferBuilder.vertex(bb.minX, bb.maxY, bb.maxZ).next();

        bufferBuilder.vertex(bb.minX, bb.maxY, bb.minZ).next();
        bufferBuilder.vertex(bb.maxX, bb.maxY, bb.minZ).next();

        bufferBuilder.vertex(bb.maxX, bb.maxY, bb.minZ).next();
        bufferBuilder.vertex(bb.maxX, bb.maxY, bb.maxZ).next();

        bufferBuilder.vertex(bb.maxX, bb.maxY, bb.maxZ).next();
        bufferBuilder.vertex(bb.minX, bb.maxY, bb.maxZ).next();

        bufferBuilder.vertex(bb.minX, bb.maxY, bb.maxZ).next();
        bufferBuilder.vertex(bb.minX, bb.maxY, bb.minZ).next();

        bufferBuilder.end();

        vertexBuffer.upload(bufferBuilder);
    }

    public static void drawSolidBox(Box bb, VertexBuffer vertexBuffer) {
        BufferBuilder bufferBuilder = Tessellator.getInstance().getBuffer();
        bufferBuilder.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION);

        bufferBuilder.vertex(bb.minX, bb.minY, bb.minZ).next();
        bufferBuilder.vertex(bb.maxX, bb.minY, bb.minZ).next();
        bufferBuilder.vertex(bb.maxX, bb.minY, bb.maxZ).next();
        bufferBuilder.vertex(bb.minX, bb.minY, bb.maxZ).next();

        bufferBuilder.vertex(bb.minX, bb.maxY, bb.minZ).next();
        bufferBuilder.vertex(bb.minX, bb.maxY, bb.maxZ).next();
        bufferBuilder.vertex(bb.maxX, bb.maxY, bb.maxZ).next();
        bufferBuilder.vertex(bb.maxX, bb.maxY, bb.minZ).next();

        bufferBuilder.vertex(bb.minX, bb.minY, bb.minZ).next();
        bufferBuilder.vertex(bb.minX, bb.maxY, bb.minZ).next();
        bufferBuilder.vertex(bb.maxX, bb.maxY, bb.minZ).next();
        bufferBuilder.vertex(bb.maxX, bb.minY, bb.minZ).next();

        bufferBuilder.vertex(bb.maxX, bb.minY, bb.minZ).next();
        bufferBuilder.vertex(bb.maxX, bb.maxY, bb.minZ).next();
        bufferBuilder.vertex(bb.maxX, bb.maxY, bb.maxZ).next();
        bufferBuilder.vertex(bb.maxX, bb.minY, bb.maxZ).next();

        bufferBuilder.vertex(bb.minX, bb.minY, bb.maxZ).next();
        bufferBuilder.vertex(bb.maxX, bb.minY, bb.maxZ).next();
        bufferBuilder.vertex(bb.maxX, bb.maxY, bb.maxZ).next();
        bufferBuilder.vertex(bb.minX, bb.maxY, bb.maxZ).next();

        bufferBuilder.vertex(bb.minX, bb.minY, bb.minZ).next();
        bufferBuilder.vertex(bb.minX, bb.minY, bb.maxZ).next();
        bufferBuilder.vertex(bb.minX, bb.maxY, bb.maxZ).next();
        bufferBuilder.vertex(bb.minX, bb.maxY, bb.minZ).next();

        bufferBuilder.end();
        vertexBuffer.upload(bufferBuilder);
    }
}
