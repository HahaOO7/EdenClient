package at.haha007.edenclient.utils;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.logging.LogUtils;
import com.mojang.math.Axis;
import fi.dy.masa.malilib.MaLiLib;
import fi.dy.masa.malilib.render.MaLiLibPipelines;
import fi.dy.masa.malilib.render.RenderContext;
import fi.dy.masa.malilib.render.RenderUtils;
import fi.dy.masa.malilib.util.data.Color4f;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.state.level.CameraEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import oshi.util.tuples.Pair;
import org.joml.Matrix4f;
import org.joml.Vector3f;

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
        RenderContext ctx = new RenderContext(() -> "edenclient:drawTracers", MaLiLibPipelines.DEBUG_LINES_MASA_SIMPLE_NO_DEPTH_NO_CULL, 0);
        BufferBuilder buffer = ctx.getBuilder();
        Vec3 origin = getTracerOrigin();
        Vec3 cameraPos = Minecraft.getInstance().gameRenderer.mainCamera().position();
        for (Vec3 pos : positions) {
            Vec3 end = pos.subtract(cameraPos);
            buffer.addVertex((float) origin.x, (float) origin.y, (float) origin.z).setColor(color.r, color.g, color.b, color.a).setLineWidth(1f);
            buffer.addVertex((float) end.x, (float) end.y, (float) end.z).setColor(color.r, color.g, color.b, color.a).setLineWidth(1f);
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

    private static Vec3 getTracerOrigin() {
        Minecraft mc = Minecraft.getInstance();
        CameraRenderState cameraState = mc.gameRenderer.gameRenderState().levelRenderState.cameraRenderState;
        if (!cameraState.initialized) {
            return Vec3.ZERO;
        }
        double damageTilt = mc.gameRenderer.gameRenderState().optionsRenderState.damageTiltStrength;
        Matrix4f pose = viewPose(cameraState.entityRenderState, mc.options.bobView().get(), damageTilt);
        return tracerOrigin(cameraState.viewRotationMatrix, pose);
    }

    static Matrix4f viewPose(CameraEntityRenderState state, boolean bobView, double damageTiltStrength) {
        Matrix4f pose = new Matrix4f();
        if (state.isLiving) {
            if (state.isDeadOrDying) {
                float duration = Math.min(state.deathTime, 20.0F);
                Axis.ZP.rotateDegrees(pose, 40.0F - 8000.0F / (duration + 200.0F));
            }
            if (state.hurtTime >= 0.0F) {
                float hurt = state.hurtTime / state.hurtDuration;
                hurt = Mth.sin(hurt * hurt * hurt * hurt * (float) Math.PI);
                Axis.YP.rotateDegrees(pose, -state.hurtDir);
                Axis.ZP.rotateDegrees(pose, (float) (-hurt * 14.0 * damageTiltStrength));
                Axis.YP.rotateDegrees(pose, state.hurtDir);
            }
        }
        if (bobView && state.isPlayer) {
            float walkDistance = state.backwardsInterpolatedWalkDistance;
            float bob = state.bob;
            pose.translate(Mth.sin(walkDistance * (float) Math.PI) * bob * 0.5F,
                    -Math.abs(Mth.cos(walkDistance * (float) Math.PI) * bob),
                    0.0F);
            Axis.ZP.rotateDegrees(pose, Mth.sin(walkDistance * (float) Math.PI) * bob * 3.0F);
            Axis.XP.rotateDegrees(pose, Math.abs(Mth.cos(walkDistance * (float) Math.PI - 0.2F) * bob) * 5.0F);
        }
        return pose;
    }

    static Vec3 tracerOrigin(Matrix4f viewRotation, Matrix4f pose) {
        Matrix4f invertedPose = new Matrix4f();
        pose.invert(invertedPose);
        Vector3f origin = new Vector3f();
        invertedPose.transformPosition(origin.set(0.0F, 0.0F, -1.0F));
        Matrix4f transposedView = new Matrix4f();
        viewRotation.transpose(transposedView);
        transposedView.transformPosition(origin);
        return new Vec3(origin.x, origin.y, origin.z);
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
        RenderContext ctx = new RenderContext(() -> "edenclient:drawBoundingBoxEdges", MaLiLibPipelines.DEBUG_LINES_MASA_SIMPLE_NO_CULL, 0);
        BufferBuilder buffer = ctx.getBuilder();

        drawBoundingBoxLinesX(buffer, minX, minY, minZ, maxX, maxY, maxZ, color);
        drawBoundingBoxLinesY(buffer, minX, minY, minZ, maxX, maxY, maxZ, color);
        drawBoundingBoxLinesZ(buffer, minX, minY, minZ, maxX, maxY, maxZ, color);

        try {
            MeshData meshData = buffer.build();
            if (meshData != null) {
                ctx.draw(meshData, false, true);
                meshData.close();
            }
            ctx.close();
        } catch (Exception err) {
            MaLiLib.LOGGER.error("drawBoundingBoxEdges(): Draw Exception; {}", err.getMessage());
        }
    }

    public static void drawLines(List<Pair<Vec3, Vec3>> lines, Color4f color) {
        drawLines(lines, color, 1f);
    }

    public static void drawLines(List<Pair<Vec3, Vec3>> lines, Color4f color, float lineWidth) {
        try (RenderContext ctx = new RenderContext(() -> "edenclient:drawLines", MaLiLibPipelines.DEBUG_LINES_MASA_SIMPLE_NO_CULL, 0)) {
            BufferBuilder buffer = ctx.getBuilder();
            for (Pair<Vec3, Vec3> line : lines) {
                Vec3 start = line.getA().subtract(RenderUtils.camPos());
                Vec3 end = line.getB().subtract(RenderUtils.camPos());
                buffer.addVertex((float) start.x, (float) start.y, (float) start.z)
                        .setColor(color.r, color.g, color.b, color.a)
                        .setLineWidth(lineWidth);
                buffer.addVertex((float) end.x, (float) end.y, (float) end.z)
                        .setColor(color.r, color.g, color.b, color.a)
                        .setLineWidth(lineWidth);
            }
            MeshData meshData = buffer.build();
            if (meshData != null) {
                ctx.draw(meshData, false, false);
                meshData.close();
            }
        } catch (Exception err) {
            LogUtils.getLogger().error("Could not draw lines", err);
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
        buffer.addVertex(minX, minY, minZ).setColor(color.r, color.g, color.b, color.a).setLineWidth(1f);
        buffer.addVertex(maxX, minY, minZ).setColor(color.r, color.g, color.b, color.a).setLineWidth(1f);

        buffer.addVertex(minX, maxY, minZ).setColor(color.r, color.g, color.b, color.a).setLineWidth(1f);
        buffer.addVertex(maxX, maxY, minZ).setColor(color.r, color.g, color.b, color.a).setLineWidth(1f);

        buffer.addVertex(minX, minY, maxZ).setColor(color.r, color.g, color.b, color.a).setLineWidth(1f);
        buffer.addVertex(maxX, minY, maxZ).setColor(color.r, color.g, color.b, color.a).setLineWidth(1f);

        buffer.addVertex(minX, maxY, maxZ).setColor(color.r, color.g, color.b, color.a).setLineWidth(1f);
        buffer.addVertex(maxX, maxY, maxZ).setColor(color.r, color.g, color.b, color.a).setLineWidth(1f);
    }

    private static void drawBoundingBoxLinesY(BufferBuilder buffer, float minX, float minY, float minZ, float maxX, float maxY, float maxZ,
                                              Color4f color) {
        buffer.addVertex(minX, minY, minZ).setColor(color.r, color.g, color.b, color.a).setLineWidth(1f);
        buffer.addVertex(minX, maxY, minZ).setColor(color.r, color.g, color.b, color.a).setLineWidth(1f);

        buffer.addVertex(maxX, minY, minZ).setColor(color.r, color.g, color.b, color.a).setLineWidth(1f);
        buffer.addVertex(maxX, maxY, minZ).setColor(color.r, color.g, color.b, color.a).setLineWidth(1f);

        buffer.addVertex(minX, minY, maxZ).setColor(color.r, color.g, color.b, color.a).setLineWidth(1f);
        buffer.addVertex(minX, maxY, maxZ).setColor(color.r, color.g, color.b, color.a).setLineWidth(1f);

        buffer.addVertex(maxX, minY, maxZ).setColor(color.r, color.g, color.b, color.a).setLineWidth(1f);
        buffer.addVertex(maxX, maxY, maxZ).setColor(color.r, color.g, color.b, color.a).setLineWidth(1f);
    }

    private static void drawBoundingBoxLinesZ(BufferBuilder buffer, float minX, float minY, float minZ, float maxX, float maxY, float maxZ,
                                              Color4f color) {
        buffer.addVertex(minX, minY, minZ).setColor(color.r, color.g, color.b, color.a).setLineWidth(1f);
        buffer.addVertex(minX, minY, maxZ).setColor(color.r, color.g, color.b, color.a).setLineWidth(1f);

        buffer.addVertex(maxX, minY, minZ).setColor(color.r, color.g, color.b, color.a).setLineWidth(1f);
        buffer.addVertex(maxX, minY, maxZ).setColor(color.r, color.g, color.b, color.a).setLineWidth(1f);

        buffer.addVertex(minX, maxY, minZ).setColor(color.r, color.g, color.b, color.a).setLineWidth(1f);
        buffer.addVertex(minX, maxY, maxZ).setColor(color.r, color.g, color.b, color.a).setLineWidth(1f);

        buffer.addVertex(maxX, maxY, minZ).setColor(color.r, color.g, color.b, color.a).setLineWidth(1f);
        buffer.addVertex(maxX, maxY, maxZ).setColor(color.r, color.g, color.b, color.a).setLineWidth(1f);
    }

}
