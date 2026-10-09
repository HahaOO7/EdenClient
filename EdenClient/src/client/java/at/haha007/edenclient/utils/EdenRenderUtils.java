package at.haha007.edenclient.utils;

import at.haha007.edenclient.render.EdenColor;
import at.haha007.edenclient.render.EdenRenderTypes;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.logging.LogUtils;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.CameraEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import oshi.util.tuples.Pair;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.slf4j.Logger;

import java.util.List;
import java.util.Optional;

/**
 * Submit-based line renderer that draws through terrain using an EdenClient-owned
 * no-depth / no-cull pipeline. Geometry is submitted during
 * {@code LevelRenderEvents.COLLECT_SUBMITS} via {@link #beginFrame} / {@link #endFrame}.
 */
public final class EdenRenderUtils {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static SubmitNodeCollector collector;

    private EdenRenderUtils() {
    }

    public static void beginFrame(SubmitNodeCollector submitNodeCollector) {
        collector = submitNodeCollector;
    }

    public static void endFrame() {
        collector = null;
    }

    /** Camera-entity eye position, or null if there is no camera entity. */
    public static Vec3 getCameraPos() {
        return Optional.ofNullable(Minecraft.getInstance().getCameraEntity())
                .map(Entity::getEyePosition)
                .orElse(null);
    }

    private static Vec3 camPos() {
        return Minecraft.getInstance().gameRenderer.mainCamera().position();
    }

    public static void drawTracers(List<Vec3> positions, EdenColor color) {
        SubmitNodeCollector target = collector;
        if (target == null) {
            LOGGER.debug("drawTracers() called outside of a render frame");
            return;
        }
        Vec3 origin = getTracerOrigin();
        Vec3 cameraPos = camPos();
        target.submitCustomGeometry(new PoseStack(), EdenRenderTypes.LINES_NO_DEPTH_NO_CULL, (pose, buffer) -> {
            for (Vec3 pos : positions) {
                Vec3 end = pos.subtract(cameraPos);
                addLine(buffer, pose,
                        (float) origin.x, (float) origin.y, (float) origin.z,
                        (float) end.x, (float) end.y, (float) end.z, color);
            }
        });
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

    public static void drawAreaOutline(Vec3 pos1, Vec3 pos2, EdenColor color) {
        Vec3 cameraPos = camPos();

        float minX = (float) (Math.min(pos1.x(), pos2.x()) - cameraPos.x);
        float minY = (float) (Math.min(pos1.y(), pos2.y()) - cameraPos.y);
        float minZ = (float) (Math.min(pos1.z(), pos2.z()) - cameraPos.z);
        float maxX = (float) (Math.max(pos1.x(), pos2.x()) - cameraPos.x);
        float maxY = (float) (Math.max(pos1.y(), pos2.y()) - cameraPos.y);
        float maxZ = (float) (Math.max(pos1.z(), pos2.z()) - cameraPos.z);

        drawBoxEdges(minX, minY, minZ, maxX, maxY, maxZ, color);
    }

    /** Draws an outline of the block at {@code pos}, expanded by {@code expand} on every side. */
    public static void drawBlockOutline(BlockPos pos, float expand, float lineWidth, EdenColor color) {
        Vec3 cameraPos = camPos();

        float minX = (float) (pos.getX() - cameraPos.x - expand);
        float minY = (float) (pos.getY() - cameraPos.y - expand);
        float minZ = (float) (pos.getZ() - cameraPos.z - expand);
        float maxX = (float) (pos.getX() - cameraPos.x + expand + 1);
        float maxY = (float) (pos.getY() - cameraPos.y + expand + 1);
        float maxZ = (float) (pos.getZ() - cameraPos.z + expand + 1);

        drawBoxEdges(minX, minY, minZ, maxX, maxY, maxZ, color);
    }

    /** Draws an outline of the block area between {@code pos1} and {@code pos2} (inclusive). */
    public static void drawAreaOutline(BlockPos pos1, BlockPos pos2, float lineWidth, EdenColor color) {
        Vec3 cameraPos = camPos();

        float minX = (float) (Math.min(pos1.getX(), pos2.getX()) - cameraPos.x);
        float minY = (float) (Math.min(pos1.getY(), pos2.getY()) - cameraPos.y);
        float minZ = (float) (Math.min(pos1.getZ(), pos2.getZ()) - cameraPos.z);
        float maxX = (float) (Math.max(pos1.getX(), pos2.getX()) - cameraPos.x + 1);
        float maxY = (float) (Math.max(pos1.getY(), pos2.getY()) - cameraPos.y + 1);
        float maxZ = (float) (Math.max(pos1.getZ(), pos2.getZ()) - cameraPos.z + 1);

        drawBoxEdges(minX, minY, minZ, maxX, maxY, maxZ, color);
    }

    public static void drawLines(List<Pair<Vec3, Vec3>> lines, EdenColor color) {
        drawLines(lines, color, 1f);
    }

    public static void drawLines(List<Pair<Vec3, Vec3>> lines, EdenColor color, float lineWidth) {
        SubmitNodeCollector target = collector;
        if (target == null) {
            LOGGER.debug("drawLines() called outside of a render frame");
            return;
        }
        Vec3 cameraPos = camPos();
        target.submitCustomGeometry(new PoseStack(), EdenRenderTypes.LINES_NO_DEPTH_NO_CULL, (pose, buffer) -> {
            for (Pair<Vec3, Vec3> line : lines) {
                Vec3 start = line.getA().subtract(cameraPos);
                Vec3 end = line.getB().subtract(cameraPos);
                addLine(buffer, pose,
                        (float) start.x, (float) start.y, (float) start.z,
                        (float) end.x, (float) end.y, (float) end.z, color);
            }
        });
    }

    private static void drawBoxEdges(float minX, float minY, float minZ, float maxX, float maxY, float maxZ, EdenColor color) {
        SubmitNodeCollector target = collector;
        if (target == null) {
            LOGGER.debug("drawBoxEdges() called outside of a render frame");
            return;
        }
        target.submitCustomGeometry(new PoseStack(), EdenRenderTypes.LINES_NO_DEPTH_NO_CULL, (pose, buffer) -> {
            // West side
            addLine(buffer, pose, minX, minY, minZ, minX, minY, maxZ, color);
            addLine(buffer, pose, minX, minY, maxZ, minX, maxY, maxZ, color);
            addLine(buffer, pose, minX, maxY, maxZ, minX, maxY, minZ, color);
            addLine(buffer, pose, minX, maxY, minZ, minX, minY, minZ, color);

            // East side
            addLine(buffer, pose, maxX, minY, maxZ, maxX, minY, minZ, color);
            addLine(buffer, pose, maxX, minY, minZ, maxX, maxY, minZ, color);
            addLine(buffer, pose, maxX, maxY, minZ, maxX, maxY, maxZ, color);
            addLine(buffer, pose, maxX, maxY, maxZ, maxX, minY, maxZ, color);

            // North side (vertical lines are already drawn by the east/west sides)
            addLine(buffer, pose, maxX, minY, minZ, minX, minY, minZ, color);
            addLine(buffer, pose, minX, maxY, minZ, maxX, maxY, minZ, color);

            // South side (vertical lines are already drawn by the east/west sides)
            addLine(buffer, pose, minX, minY, maxZ, maxX, minY, maxZ, color);
            addLine(buffer, pose, maxX, maxY, maxZ, minX, maxY, maxZ, color);
        });
    }

    private static void addLine(VertexConsumer buffer, PoseStack.Pose pose,
                                float x1, float y1, float z1, float x2, float y2, float z2, EdenColor color) {
        buffer.addVertex(pose, x1, y1, z1).setColor(color.r(), color.g(), color.b(), color.a());
        buffer.addVertex(pose, x2, y2, z2).setColor(color.r(), color.g(), color.b(), color.a());
    }
}
