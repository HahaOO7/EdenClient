package at.haha007.edenclient.utils;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.VertexBuffer;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.Vec3d;

public enum RenderUtils {
    ;

    public static void applyRegionalRenderOffset(MatrixStack matrixStack) {

        Vec3d camPos = getCameraPos();
        BlockPos blockPos = getCameraBlockPos();

        int regionX = (blockPos.getX() >> 9) * 512;
        int regionZ = (blockPos.getZ() >> 9) * 512;

        matrixStack.translate(regionX - camPos.x, -camPos.y,
                regionZ - camPos.z);
    }

    public static Vec3d getCameraPos() {
        return MinecraftClient.getInstance().getBlockEntityRenderDispatcher().camera.getPos();
    }

    public static BlockPos getCameraBlockPos() {
        return MinecraftClient.getInstance().getBlockEntityRenderDispatcher().camera
                .getBlockPos();
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

    public static void drawSolidBox(Box bb, MatrixStack matrixStack, float r, float g, float b) {
        Matrix4f matrix = matrixStack.peek().getModel();
        BufferBuilder bufferBuilder = Tessellator.getInstance().getBuffer();
        RenderSystem.setShader(GameRenderer::getPositionShader);
        bufferBuilder.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION);

        bufferBuilder.vertex(matrix, (float) bb.minX, (float) bb.minY, (float) bb.minZ).next();
        bufferBuilder.vertex(matrix, (float) bb.maxX, (float) bb.minY, (float) bb.minZ).next();
        bufferBuilder.vertex(matrix, (float) bb.maxX, (float) bb.minY, (float) bb.maxZ).next();
        bufferBuilder.vertex(matrix, (float) bb.minX, (float) bb.minY, (float) bb.maxZ).next();

        bufferBuilder.vertex(matrix, (float) bb.minX, (float) bb.maxY, (float) bb.minZ).next();
        bufferBuilder.vertex(matrix, (float) bb.minX, (float) bb.maxY, (float) bb.maxZ).next();
        bufferBuilder.vertex(matrix, (float) bb.maxX, (float) bb.maxY, (float) bb.maxZ).next();
        bufferBuilder.vertex(matrix, (float) bb.maxX, (float) bb.maxY, (float) bb.minZ).next();

        bufferBuilder.vertex(matrix, (float) bb.minX, (float) bb.minY, (float) bb.minZ).next();
        bufferBuilder.vertex(matrix, (float) bb.minX, (float) bb.maxY, (float) bb.minZ).next();
        bufferBuilder.vertex(matrix, (float) bb.maxX, (float) bb.maxY, (float) bb.minZ).next();
        bufferBuilder.vertex(matrix, (float) bb.maxX, (float) bb.minY, (float) bb.minZ).next();

        bufferBuilder.vertex(matrix, (float) bb.maxX, (float) bb.minY, (float) bb.minZ).next();
        bufferBuilder.vertex(matrix, (float) bb.maxX, (float) bb.maxY, (float) bb.minZ).next();
        bufferBuilder.vertex(matrix, (float) bb.maxX, (float) bb.maxY, (float) bb.maxZ).next();
        bufferBuilder.vertex(matrix, (float) bb.maxX, (float) bb.minY, (float) bb.maxZ).next();

        bufferBuilder.vertex(matrix, (float) bb.minX, (float) bb.minY, (float) bb.maxZ).next();
        bufferBuilder.vertex(matrix, (float) bb.maxX, (float) bb.minY, (float) bb.maxZ).next();
        bufferBuilder.vertex(matrix, (float) bb.maxX, (float) bb.maxY, (float) bb.maxZ).next();
        bufferBuilder.vertex(matrix, (float) bb.minX, (float) bb.maxY, (float) bb.maxZ).next();

        bufferBuilder.vertex(matrix, (float) bb.minX, (float) bb.minY, (float) bb.minZ).next();
        bufferBuilder.vertex(matrix, (float) bb.minX, (float) bb.minY, (float) bb.maxZ).next();
        bufferBuilder.vertex(matrix, (float) bb.minX, (float) bb.maxY, (float) bb.maxZ).next();
        bufferBuilder.vertex(matrix, (float) bb.minX, (float) bb.maxY, (float) bb.minZ).next();

        bufferBuilder.end();
        RenderSystem.setShaderColor(r, g, b, 1);
        BufferRenderer.draw(bufferBuilder);
    }

    public static void drawOutlinedBox(Box bb, MatrixStack matrixStack, float r, float g, float b) {
        Matrix4f matrix = matrixStack.peek().getModel();
        BufferBuilder bufferBuilder = Tessellator.getInstance().getBuffer();
        RenderSystem.setShader(GameRenderer::getPositionShader);

        bufferBuilder.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION);
        bufferBuilder.vertex(matrix, (float) bb.minX, (float) bb.minY, (float) bb.minZ).next();
        bufferBuilder.vertex(matrix, (float) bb.maxX, (float) bb.minY, (float) bb.minZ).next();

        bufferBuilder.vertex(matrix, (float) bb.maxX, (float) bb.minY, (float) bb.minZ).next();
        bufferBuilder.vertex(matrix, (float) bb.maxX, (float) bb.minY, (float) bb.maxZ).next();

        bufferBuilder.vertex(matrix, (float) bb.maxX, (float) bb.minY, (float) bb.maxZ).next();
        bufferBuilder.vertex(matrix, (float) bb.minX, (float) bb.minY, (float) bb.maxZ).next();

        bufferBuilder.vertex(matrix, (float) bb.minX, (float) bb.minY, (float) bb.maxZ).next();
        bufferBuilder.vertex(matrix, (float) bb.minX, (float) bb.minY, (float) bb.minZ).next();

        bufferBuilder.vertex(matrix, (float) bb.minX, (float) bb.minY, (float) bb.minZ).next();
        bufferBuilder.vertex(matrix, (float) bb.minX, (float) bb.maxY, (float) bb.minZ).next();

        bufferBuilder.vertex(matrix, (float) bb.maxX, (float) bb.minY, (float) bb.minZ).next();
        bufferBuilder.vertex(matrix, (float) bb.maxX, (float) bb.maxY, (float) bb.minZ).next();

        bufferBuilder.vertex(matrix, (float) bb.maxX, (float) bb.minY, (float) bb.maxZ).next();
        bufferBuilder.vertex(matrix, (float) bb.maxX, (float) bb.maxY, (float) bb.maxZ).next();

        bufferBuilder.vertex(matrix, (float) bb.minX, (float) bb.minY, (float) bb.maxZ).next();
        bufferBuilder.vertex(matrix, (float) bb.minX, (float) bb.maxY, (float) bb.maxZ).next();

        bufferBuilder.vertex(matrix, (float) bb.minX, (float) bb.maxY, (float) bb.minZ).next();
        bufferBuilder.vertex(matrix, (float) bb.maxX, (float) bb.maxY, (float) bb.minZ).next();

        bufferBuilder.vertex(matrix, (float) bb.maxX, (float) bb.maxY, (float) bb.minZ).next();
        bufferBuilder.vertex(matrix, (float) bb.maxX, (float) bb.maxY, (float) bb.maxZ).next();

        bufferBuilder.vertex(matrix, (float) bb.maxX, (float) bb.maxY, (float) bb.maxZ).next();
        bufferBuilder.vertex(matrix, (float) bb.minX, (float) bb.maxY, (float) bb.maxZ).next();

        bufferBuilder.vertex(matrix, (float) bb.minX, (float) bb.maxY, (float) bb.maxZ).next();
        bufferBuilder.vertex(matrix, (float) bb.minX, (float) bb.maxY, (float) bb.minZ).next();
        bufferBuilder.end();
        RenderSystem.setShaderColor(r, g, b, 1);
        BufferRenderer.draw(bufferBuilder);
    }

}
