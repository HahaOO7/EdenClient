package at.haha007.edenclient.utils;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import fi.dy.masa.malilib.MaLiLib;
import fi.dy.masa.malilib.render.MaLiLibPipelines;
import fi.dy.masa.malilib.render.RenderContext;
import fi.dy.masa.malilib.render.RenderUtils;
import fi.dy.masa.malilib.util.data.Color4f;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import oshi.util.tuples.Pair;

import java.util.List;
import java.util.Optional;

public enum EdenRenderUtils {
    ;

    public static Vec3 getCameraPos() {
        return Optional.ofNullable(Minecraft.getInstance().getCameraEntity())
                .map(Entity::getEyePosition)
                .orElse(null);
    }

    public static void drawTracers(List<Vec3> positions, Color4f color) {
        RenderContext ctx = new RenderContext(() -> "edenclient:drawTracers", MaLiLibPipelines.DEBUG_LINES_MASA_SIMPLE_NO_DEPTH_NO_CULL);
        BufferBuilder buffer = ctx.getBuilder();
        Vec3 eyePos = PlayerUtils.getClientLookVec();
        Vec3 cameraPos = fi.dy.masa.malilib.render.RenderUtils.camPos();
        for (Vec3 pos : positions) {
            pos = pos.subtract(cameraPos);
            buffer.addVertex((float) eyePos.x, (float) eyePos.y, (float) eyePos.z).setColor(color.r, color.g, color.b, color.a);
            buffer.addVertex((float) pos.x, (float) pos.y, (float) pos.z).setColor(color.r, color.g, color.b, color.a);
        }
        try {
            MeshData meshData = buffer.build();
            if (meshData != null) {
                ctx.draw(meshData, false, false);
                meshData.close();
            }
            ctx.close();
        } catch (Exception err) {
            MaLiLib.LOGGER.error("renderBlockOutline(): Draw Exception; {}", err.getMessage());
        }
    }

    public static void drawAreaOutline(Vec3 pos1, Vec3 pos2, Color4f color) {
        Vec3 cameraPos = RenderUtils.camPos();
        final double dx = cameraPos.x;
        final double dy = cameraPos.y;
        final double dz = cameraPos.z;

        double minX = Math.min(pos1.x(), pos2.x()) - dx;
        double minY = Math.min(pos1.y(), pos2.y()) - dy;
        double minZ = Math.min(pos1.z(), pos2.z()) - dz;
        double maxX = Math.max(pos1.x(), pos2.x()) - dx;
        double maxY = Math.max(pos1.y(), pos2.y()) - dy;
        double maxZ = Math.max(pos1.z(), pos2.z()) - dz;

        drawBoundingBoxEdges((float) minX, (float) minY, (float) minZ, (float) maxX, (float) maxY, (float) maxZ, color);
    }

    private static void drawBoundingBoxEdges(float minX,
                                             float minY,
                                             float minZ,
                                             float maxX,
                                             float maxY,
                                             float maxZ,
                                             Color4f color) {
        RenderContext ctx = new RenderContext(() -> "edenclient:drawBoundingBoxEdges", MaLiLibPipelines.DEBUG_LINES_MASA_SIMPLE_NO_DEPTH_NO_CULL);
        BufferBuilder buffer = ctx.getBuilder();

        drawBoundingBoxLinesX(buffer, minX, minY, minZ, maxX, maxY, maxZ, color);
        drawBoundingBoxLinesY(buffer, minX, minY, minZ, maxX, maxY, maxZ, color);
        drawBoundingBoxLinesZ(buffer, minX, minY, minZ, maxX, maxY, maxZ, color);

        try {
            MeshData meshData = buffer.build();
            if (meshData != null) {
                ctx.draw(meshData, false, false);
                meshData.close();
            }
            ctx.close();
        } catch (Exception err) {
            MaLiLib.LOGGER.error("drawBoundingBoxEdges(): Draw Exception; {}", err.getMessage());
        }
    }

    public static void drawLines(List<Pair<Vec3, Vec3>> lines, Color4f color) {
        // MaLiLibPipelines.LINES_NO_DEPTH_NO_CULL
        RenderContext ctx = new RenderContext(() -> "edenclient:drawBoundingBoxEdges", MaLiLibPipelines.DEBUG_LINES_MASA_SIMPLE_NO_CULL);
        BufferBuilder buffer = ctx.getBuilder();

        for (Pair<Vec3, Vec3> line : lines) {
            Vec3 start = line.getA().subtract(RenderUtils.camPos());
            Vec3 end = line.getB().subtract(RenderUtils.camPos());
            buffer.addVertex((float) start.x, (float) start.y, (float) start.z).setColor(color.r, color.g, color.b, color.a);
            buffer.addVertex((float) end.x, (float) end.y, (float) end.z).setColor(color.r, color.g, color.b, color.a);
        }

        try {
            MeshData meshData = buffer.build();
            if (meshData != null) {
                ctx.draw(meshData, false, false);
                meshData.close();
            }
            ctx.close();
        } catch (Exception err) {
            MaLiLib.LOGGER.error("drawBoundingBoxEdges(): Draw Exception; {}", err.getMessage());
        }
    }

    private static void drawBoundingBoxLinesX(BufferBuilder buffer,
                                              float minX,
                                              float minY,
                                              float minZ,
                                              float maxX,
                                              float maxY,
                                              float maxZ,
                                              Color4f color) {
        buffer.addVertex(minX, minY, minZ).setColor(color.r, color.g, color.b, color.a);
        buffer.addVertex(maxX, minY, minZ).setColor(color.r, color.g, color.b, color.a);

        buffer.addVertex(minX, maxY, minZ).setColor(color.r, color.g, color.b, color.a);
        buffer.addVertex(maxX, maxY, minZ).setColor(color.r, color.g, color.b, color.a);

        buffer.addVertex(minX, minY, maxZ).setColor(color.r, color.g, color.b, color.a);
        buffer.addVertex(maxX, minY, maxZ).setColor(color.r, color.g, color.b, color.a);

        buffer.addVertex(minX, maxY, maxZ).setColor(color.r, color.g, color.b, color.a);
        buffer.addVertex(maxX, maxY, maxZ).setColor(color.r, color.g, color.b, color.a);
    }

    private static void drawBoundingBoxLinesY(BufferBuilder buffer, float minX, float minY, float minZ, float maxX, float maxY, float maxZ,
                                              Color4f color) {
        buffer.addVertex(minX, minY, minZ).setColor(color.r, color.g, color.b, color.a);
        buffer.addVertex(minX, maxY, minZ).setColor(color.r, color.g, color.b, color.a);

        buffer.addVertex(maxX, minY, minZ).setColor(color.r, color.g, color.b, color.a);
        buffer.addVertex(maxX, maxY, minZ).setColor(color.r, color.g, color.b, color.a);

        buffer.addVertex(minX, minY, maxZ).setColor(color.r, color.g, color.b, color.a);
        buffer.addVertex(minX, maxY, maxZ).setColor(color.r, color.g, color.b, color.a);

        buffer.addVertex(maxX, minY, maxZ).setColor(color.r, color.g, color.b, color.a);
        buffer.addVertex(maxX, maxY, maxZ).setColor(color.r, color.g, color.b, color.a);
    }

    private static void drawBoundingBoxLinesZ(BufferBuilder buffer, float minX, float minY, float minZ, float maxX, float maxY, float maxZ,
                                              Color4f color) {
        buffer.addVertex(minX, minY, minZ).setColor(color.r, color.g, color.b, color.a);
        buffer.addVertex(minX, minY, maxZ).setColor(color.r, color.g, color.b, color.a);

        buffer.addVertex(maxX, minY, minZ).setColor(color.r, color.g, color.b, color.a);
        buffer.addVertex(maxX, minY, maxZ).setColor(color.r, color.g, color.b, color.a);

        buffer.addVertex(minX, maxY, minZ).setColor(color.r, color.g, color.b, color.a);
        buffer.addVertex(minX, maxY, maxZ).setColor(color.r, color.g, color.b, color.a);

        buffer.addVertex(maxX, maxY, minZ).setColor(color.r, color.g, color.b, color.a);
        buffer.addVertex(maxX, maxY, maxZ).setColor(color.r, color.g, color.b, color.a);
    }

}
