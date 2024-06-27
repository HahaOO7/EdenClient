package at.haha007.edenclient.utils;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public enum RenderUtils {
    ;

    public static Vec3 getCameraPos() {
        return Minecraft.getInstance().getBlockEntityRenderDispatcher().camera.getPosition();
    }

    public static Camera getCamera() {
        return Minecraft.getInstance().getBlockEntityRenderDispatcher().camera;
    }

    public static void drawOutlinedBox(AABB bb, VertexBuffer vertexBuffer) {
        BufferBuilder bufferBuilder = Tesselator.getInstance().begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION);

        bufferBuilder.addVertex((float) bb.minX, (float) bb.minY, (float) bb.minZ);
        bufferBuilder.addVertex((float) bb.maxX, (float) bb.minY, (float) bb.minZ);

        bufferBuilder.addVertex((float) bb.maxX, (float) bb.minY, (float) bb.minZ);
        bufferBuilder.addVertex((float) bb.maxX, (float) bb.minY, (float) bb.maxZ);

        bufferBuilder.addVertex((float) bb.maxX, (float) bb.minY, (float) bb.maxZ);
        bufferBuilder.addVertex((float) bb.minX, (float) bb.minY, (float) bb.maxZ);

        bufferBuilder.addVertex((float) bb.minX, (float) bb.minY, (float) bb.maxZ);
        bufferBuilder.addVertex((float) bb.minX, (float) bb.minY, (float) bb.minZ);

        bufferBuilder.addVertex((float) bb.minX, (float) bb.minY, (float) bb.minZ);
        bufferBuilder.addVertex((float) bb.minX, (float) bb.maxY, (float) bb.minZ);

        bufferBuilder.addVertex((float) bb.maxX, (float) bb.minY, (float) bb.minZ);
        bufferBuilder.addVertex((float) bb.maxX, (float) bb.maxY, (float) bb.minZ);

        bufferBuilder.addVertex((float) bb.maxX, (float) bb.minY, (float) bb.maxZ);
        bufferBuilder.addVertex((float) bb.maxX, (float) bb.maxY, (float) bb.maxZ);

        bufferBuilder.addVertex((float) bb.minX, (float) bb.minY, (float) bb.maxZ);
        bufferBuilder.addVertex((float) bb.minX, (float) bb.maxY, (float) bb.maxZ);

        bufferBuilder.addVertex((float) bb.minX, (float) bb.maxY, (float) bb.minZ);
        bufferBuilder.addVertex((float) bb.maxX, (float) bb.maxY, (float) bb.minZ);

        bufferBuilder.addVertex((float) bb.maxX, (float) bb.maxY, (float) bb.minZ);
        bufferBuilder.addVertex((float) bb.maxX, (float) bb.maxY, (float) bb.maxZ);

        bufferBuilder.addVertex((float) bb.maxX, (float) bb.maxY, (float) bb.maxZ);
        bufferBuilder.addVertex((float) bb.minX, (float) bb.maxY, (float) bb.maxZ);

        bufferBuilder.addVertex((float) bb.minX, (float) bb.maxY, (float) bb.maxZ);
        bufferBuilder.addVertex((float) bb.minX, (float) bb.maxY, (float) bb.minZ);
        vertexBuffer.bind();
        vertexBuffer.upload(bufferBuilder.build());
        VertexBuffer.unbind();
    }

    public static void drawSolidBox(AABB bb, VertexBuffer vertexBuffer) {
        BufferBuilder bufferBuilder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);

        bufferBuilder.addVertex((float) bb.minX, (float) bb.minY, (float) bb.minZ);
        bufferBuilder.addVertex((float) bb.maxX, (float) bb.minY, (float) bb.minZ);
        bufferBuilder.addVertex((float) bb.maxX, (float) bb.minY, (float) bb.maxZ);
        bufferBuilder.addVertex((float) bb.minX, (float) bb.minY, (float) bb.maxZ);

        bufferBuilder.addVertex((float) bb.minX, (float) bb.maxY, (float) bb.minZ);
        bufferBuilder.addVertex((float) bb.minX, (float) bb.maxY, (float) bb.maxZ);
        bufferBuilder.addVertex((float) bb.maxX, (float) bb.maxY, (float) bb.maxZ);
        bufferBuilder.addVertex((float) bb.maxX, (float) bb.maxY, (float) bb.minZ);

        bufferBuilder.addVertex((float) bb.minX, (float) bb.minY, (float) bb.minZ);
        bufferBuilder.addVertex((float) bb.minX, (float) bb.maxY, (float) bb.minZ);
        bufferBuilder.addVertex((float) bb.maxX, (float) bb.maxY, (float) bb.minZ);
        bufferBuilder.addVertex((float) bb.maxX, (float) bb.minY, (float) bb.minZ);

        bufferBuilder.addVertex((float) bb.maxX, (float) bb.minY, (float) bb.minZ);
        bufferBuilder.addVertex((float) bb.maxX, (float) bb.maxY, (float) bb.minZ);
        bufferBuilder.addVertex((float) bb.maxX, (float) bb.maxY, (float) bb.maxZ);
        bufferBuilder.addVertex((float) bb.maxX, (float) bb.minY, (float) bb.maxZ);

        bufferBuilder.addVertex((float) bb.minX, (float) bb.minY, (float) bb.maxZ);
        bufferBuilder.addVertex((float) bb.maxX, (float) bb.minY, (float) bb.maxZ);
        bufferBuilder.addVertex((float) bb.maxX, (float) bb.maxY, (float) bb.maxZ);
        bufferBuilder.addVertex((float) bb.minX, (float) bb.maxY, (float) bb.maxZ);

        bufferBuilder.addVertex((float) bb.minX, (float) bb.minY, (float) bb.minZ);
        bufferBuilder.addVertex((float) bb.minX, (float) bb.minY, (float) bb.maxZ);
        bufferBuilder.addVertex((float) bb.minX, (float) bb.maxY, (float) bb.maxZ);
        bufferBuilder.addVertex((float) bb.minX, (float) bb.maxY, (float) bb.minZ);

        vertexBuffer.bind();
        vertexBuffer.upload(bufferBuilder.build());
        VertexBuffer.unbind();
    }
}
