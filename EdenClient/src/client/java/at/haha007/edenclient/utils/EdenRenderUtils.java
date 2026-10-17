package at.haha007.edenclient.utils;

import at.haha007.edenclient.render.EdenColor;
import at.haha007.edenclient.render.EdenRenderTypes;
import at.haha007.edenclient.render.Line;
import at.haha007.edenclient.render.Quad;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.logging.LogUtils;
import com.mojang.math.Axis;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.CustomFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.slf4j.Logger;

import java.util.List;
import java.util.Optional;

public final class EdenRenderUtils {
    private static final Logger LOGGER = LogUtils.getLogger();

    public enum DepthTest {
        NO_DEPTH,
        DEPTH_TESTED
    }


    @Getter
    private static SubmitNodeCollector collector;

    private EdenRenderUtils() {
    }

    public static void beginFrame(SubmitNodeCollector submitNodeCollector) {
        collector = submitNodeCollector;
    }

    public static void endFrame() {
        collector = null;
    }

    private static void submitOverlay(RenderType renderType, SubmitNodeCollector.CustomGeometryRenderer renderer) {
        SubmitNodeCollector target = collector;
        if (target == null) {
            return;
        }
        PoseStack poseStack = new PoseStack();
        if (target instanceof SubmitNodeStorage storage) {
            storage.order(0).afterTerrain.submit(
                    new CustomFeatureRenderer.Submit(poseStack.last().copy(), renderType, renderer));
        } else {
            target.submitCustomGeometry(poseStack, renderType, renderer);
        }
    }

    /**
     * Camera-entity eye position, or null if there is no camera entity.
     */
    public static Vec3 getCameraPos() {
        return Optional.ofNullable(Minecraft.getInstance().getCameraEntity())
                .map(Entity::getEyePosition)
                .orElse(null);
    }

    private static Vec3 camPos() {
        return Minecraft.getInstance().gameRenderer.mainCamera().position();
    }

    public static void drawTracers(List<Vec3> positions, EdenColor color) {
        if (collector == null) {
            LOGGER.debug("drawTracers() called outside of a render frame");
            return;
        }
        Vec3 origin = getTracerOrigin();
        Vec3 cameraPos = camPos();
        submitOverlay(EdenRenderTypes.LINES_NO_DEPTH_NO_CULL, (pose, buffer) -> {
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

    public static void drawAreaOutline(Vec3 pos1, Vec3 pos2, float lineWidth, EdenColor color, DepthTest mode) {
        Vec3 cameraPos = camPos();

        float minX = (float) (Math.min(pos1.x(), pos2.x()) - cameraPos.x);
        float minY = (float) (Math.min(pos1.y(), pos2.y()) - cameraPos.y);
        float minZ = (float) (Math.min(pos1.z(), pos2.z()) - cameraPos.z);
        float maxX = (float) (Math.max(pos1.x(), pos2.x()) - cameraPos.x);
        float maxY = (float) (Math.max(pos1.y(), pos2.y()) - cameraPos.y);
        float maxZ = (float) (Math.max(pos1.z(), pos2.z()) - cameraPos.z);

        drawBoxEdges(minX, minY, minZ, maxX, maxY, maxZ, color, lineWidth, lineWidthRenderType(mode));
    }

    public static void drawBlockOutline(BlockPos pos, float expand, float lineWidth, EdenColor color) {
        drawBlockOutline(pos, expand, lineWidth, color, DepthTest.NO_DEPTH);
    }

    public static void drawBlockOutline(BlockPos pos, float expand, float lineWidth, EdenColor color, DepthTest mode) {
        Vec3 cameraPos = camPos();

        float minX = (float) (pos.getX() - cameraPos.x - expand);
        float minY = (float) (pos.getY() - cameraPos.y - expand);
        float minZ = (float) (pos.getZ() - cameraPos.z - expand);
        float maxX = (float) (pos.getX() - cameraPos.x + expand + 1);
        float maxY = (float) (pos.getY() - cameraPos.y + expand + 1);
        float maxZ = (float) (pos.getZ() - cameraPos.z + expand + 1);

        drawBoxEdges(minX, minY, minZ, maxX, maxY, maxZ, color, lineWidth, lineWidthRenderType(mode));
    }

    public static void drawAreaOutline(BlockPos pos1, BlockPos pos2, float lineWidth, EdenColor color) {
        drawAreaOutline(pos1, pos2, lineWidth, color, DepthTest.NO_DEPTH);
    }

    public static void drawAreaOutline(BlockPos pos1, BlockPos pos2, float lineWidth, EdenColor color, DepthTest mode) {
        Vec3 cameraPos = camPos();

        float minX = (float) (Math.min(pos1.getX(), pos2.getX()) - cameraPos.x);
        float minY = (float) (Math.min(pos1.getY(), pos2.getY()) - cameraPos.y);
        float minZ = (float) (Math.min(pos1.getZ(), pos2.getZ()) - cameraPos.z);
        float maxX = (float) (Math.max(pos1.getX(), pos2.getX()) - cameraPos.x + 1);
        float maxY = (float) (Math.max(pos1.getY(), pos2.getY()) - cameraPos.y + 1);
        float maxZ = (float) (Math.max(pos1.getZ(), pos2.getZ()) - cameraPos.z + 1);

        drawBoxEdges(minX, minY, minZ, maxX, maxY, maxZ, color, lineWidth, lineWidthRenderType(mode));
    }

    public static void drawLines(List<Line> lines, EdenColor color) {
        if (collector == null) {
            LOGGER.debug("drawLines(List<Pair<Vec3, Vec3>> lines, EdenColor color) called outside of a render frame");
            return;
        }
        Vec3 cameraPos = camPos();
        submitOverlay(EdenRenderTypes.LINES_NO_DEPTH_NO_CULL, (pose, buffer) -> {
            for (Line line : lines) {
                Vec3 start = line.start().subtract(cameraPos);
                Vec3 end = line.end().subtract(cameraPos);
                addLine(buffer, pose,
                        (float) start.x, (float) start.y, (float) start.z,
                        (float) end.x, (float) end.y, (float) end.z, color);
            }
        });
    }

    public static void drawLines(List<Line> lines, EdenColor color, float lineWidth, DepthTest mode) {
        if (collector == null) {
            LOGGER.debug("public static void drawLines(List<Pair<Vec3, Vec3>> lines, EdenColor color, float lineWidth, DepthTest mode) called outside of a render frame");
            return;
        }
        Vec3 cameraPos = camPos();
        submitOverlay(lineWidthRenderType(mode), (pose, buffer) -> {
            for (Line line : lines) {
                Vec3 start = line.start().subtract(cameraPos);
                Vec3 end = line.end().subtract(cameraPos);
                addLine(buffer, pose,
                        (float) start.x, (float) start.y, (float) start.z,
                        (float) end.x, (float) end.y, (float) end.z, color, lineWidth);
            }
        });
    }

    public static void drawFaces(List<Quad> faces, EdenColor color, DepthTest mode) {
        if (collector == null) {
            return;
        }
        Vec3 cameraPos = camPos();
        submitOverlay(fillRenderType(mode), (pose, buffer) ->
                writeFaceQuads(buffer, pose, faces, cameraPos, color));
    }

    public static void drawAreaFill(Vec3 pos1, Vec3 pos2, EdenColor color) {
        drawAreaFill(pos1, pos2, color, DepthTest.DEPTH_TESTED);
    }

    public static void drawAreaFill(Vec3 pos1, Vec3 pos2, EdenColor color, DepthTest mode) {
        if (collector == null) {
            LOGGER.debug("drawAreaFill() called outside of a render frame");
            return;
        }
        Vec3 cameraPos = camPos();

        float minX = (float) (Math.min(pos1.x(), pos2.x()) - cameraPos.x);
        float minY = (float) (Math.min(pos1.y(), pos2.y()) - cameraPos.y);
        float minZ = (float) (Math.min(pos1.z(), pos2.z()) - cameraPos.z);
        float maxX = (float) (Math.max(pos1.x(), pos2.x()) - cameraPos.x);
        float maxY = (float) (Math.max(pos1.y(), pos2.y()) - cameraPos.y);
        float maxZ = (float) (Math.max(pos1.z(), pos2.z()) - cameraPos.z);

        submitOverlay(fillRenderType(mode),
                (pose, buffer) -> writeAreaFillQuads(buffer, pose, minX, minY, minZ, maxX, maxY, maxZ, color));
    }

    public static void drawAreaFill(BlockPos pos1, BlockPos pos2, EdenColor color) {
        drawAreaFill(pos1, pos2, color, DepthTest.DEPTH_TESTED);
    }

    public static void drawAreaFill(BlockPos pos1, BlockPos pos2, EdenColor color, DepthTest mode) {
        Vec3 min = Vec3.atLowerCornerOf(new BlockPos(
                Math.min(pos1.getX(), pos2.getX()),
                Math.min(pos1.getY(), pos2.getY()),
                Math.min(pos1.getZ(), pos2.getZ())));
        Vec3 max = Vec3.atLowerCornerOf(new BlockPos(
                Math.max(pos1.getX(), pos2.getX()) + 1,
                Math.max(pos1.getY(), pos2.getY()) + 1,
                Math.max(pos1.getZ(), pos2.getZ()) + 1));
        drawAreaFill(min, max, color, mode);
    }

    private static void drawBoxEdges(float minX, float minY, float minZ,
                                     float maxX, float maxY, float maxZ,
                                     EdenColor color) {
        if (collector == null) {
            return;
        }
        submitOverlay(EdenRenderTypes.LINES_NO_DEPTH_NO_CULL, (pose, buffer) ->
                forEachBoxEdge(minX, minY, minZ, maxX, maxY, maxZ,
                        (x1, y1, z1, x2, y2, z2) -> addLine(buffer, pose, x1, y1, z1, x2, y2, z2, color)));
    }

    private static void drawBoxEdges(float minX, float minY, float minZ,
                                     float maxX, float maxY, float maxZ,
                                     EdenColor color, float lineWidth, RenderType renderType) {
        if (collector == null) {
            return;
        }
        submitOverlay(renderType, (pose, buffer) ->
                forEachBoxEdge(minX, minY, minZ, maxX, maxY, maxZ,
                        (x1, y1, z1, x2, y2, z2) -> addLine(buffer, pose, x1, y1, z1, x2, y2, z2, color, lineWidth)));
    }

    private static void forEachBoxEdge(float minX, float minY, float minZ,
                                       float maxX, float maxY, float maxZ,
                                       EdgeConsumer consumer) {
        // West side
        consumer.accept(minX, minY, minZ, minX, minY, maxZ);
        consumer.accept(minX, minY, maxZ, minX, maxY, maxZ);
        consumer.accept(minX, maxY, maxZ, minX, maxY, minZ);
        consumer.accept(minX, maxY, minZ, minX, minY, minZ);

        // East side
        consumer.accept(maxX, minY, maxZ, maxX, minY, minZ);
        consumer.accept(maxX, minY, minZ, maxX, maxY, minZ);
        consumer.accept(maxX, maxY, minZ, maxX, maxY, maxZ);
        consumer.accept(maxX, maxY, maxZ, maxX, minY, maxZ);

        // North side (vertical lines are already drawn by the east/west sides)
        consumer.accept(maxX, minY, minZ, minX, minY, minZ);
        consumer.accept(minX, maxY, minZ, maxX, maxY, minZ);

        // South side (vertical lines are already drawn by the east/west sides)
        consumer.accept(minX, minY, maxZ, maxX, minY, maxZ);
        consumer.accept(maxX, maxY, maxZ, minX, maxY, maxZ);
    }

    @FunctionalInterface
    private interface EdgeConsumer {
        void accept(float x1, float y1, float z1, float x2, float y2, float z2);
    }

    private static RenderType lineWidthRenderType(DepthTest mode) {
        return mode == DepthTest.DEPTH_TESTED
                ? EdenRenderTypes.LINES_WIDTH_DEPTH_TESTED
                : EdenRenderTypes.LINES_WIDTH_NO_DEPTH_NO_CULL;
    }

    private static RenderType fillRenderType(DepthTest mode) {
        return mode == DepthTest.NO_DEPTH
                ? EdenRenderTypes.AREA_FILL_NO_DEPTH_NO_CULL
                : EdenRenderTypes.AREA_FILL_DEPTH_TESTED;
    }

    private static void addLine(VertexConsumer buffer,
                                PoseStack.Pose pose,
                                float x1, float y1, float z1,
                                float x2, float y2, float z2,
                                EdenColor color) {
        buffer.addVertex(pose, x1, y1, z1).setColor(color.r(), color.g(), color.b(), color.a());
        buffer.addVertex(pose, x2, y2, z2).setColor(color.r(), color.g(), color.b(), color.a());
    }

    static void addLine(VertexConsumer buffer,
                        PoseStack.Pose pose,
                        float x1, float y1, float z1,
                        float x2, float y2, float z2,
                        EdenColor color,
                        float lineWidth) {
        Vector3f direction = new Vector3f(x2 - x1, y2 - y1, z2 - z1);
        if (direction.lengthSquared() > 0) {
            direction.normalize();
        }
        buffer.addVertex(pose, x1, y1, z1).setColor(color.r(), color.g(), color.b(), color.a())
                .setNormal(pose, direction).setLineWidth(lineWidth);
        buffer.addVertex(pose, x2, y2, z2).setColor(color.r(), color.g(), color.b(), color.a())
                .setNormal(pose, direction).setLineWidth(lineWidth);
    }

    static void writeAreaFillQuads(VertexConsumer buffer,
                                   PoseStack.Pose pose,
                                   float minX, float minY, float minZ,
                                   float maxX, float maxY, float maxZ,
                                   EdenColor color) {
        // West (x = minX)
        addQuad(buffer, pose, minX, minY, minZ, minX, maxY, minZ, minX, maxY, maxZ, minX, minY, maxZ, color);
        // East (x = maxX)
        addQuad(buffer, pose, maxX, minY, minZ, maxX, minY, maxZ, maxX, maxY, maxZ, maxX, maxY, minZ, color);
        // North (z = minZ)
        addQuad(buffer, pose, minX, minY, minZ, maxX, minY, minZ, maxX, maxY, minZ, minX, maxY, minZ, color);
        // South (z = maxZ)
        addQuad(buffer, pose, minX, minY, maxZ, minX, maxY, maxZ, maxX, maxY, maxZ, maxX, minY, maxZ, color);
        // Bottom (y = minY)
        addQuad(buffer, pose, minX, minY, minZ, minX, minY, maxZ, maxX, minY, maxZ, maxX, minY, minZ, color);
        // Top (y = maxY)
        addQuad(buffer, pose, minX, maxY, minZ, maxX, maxY, minZ, maxX, maxY, maxZ, minX, maxY, maxZ, color);
    }

    private static void addQuad(VertexConsumer buffer, PoseStack.Pose pose,
                                float x0, float y0, float z0,
                                float x1, float y1, float z1,
                                float x2, float y2, float z2,
                                float x3, float y3, float z3,
                                EdenColor color) {
        buffer.addVertex(pose, x0, y0, z0).setColor(color.r(), color.g(), color.b(), color.a());
        buffer.addVertex(pose, x1, y1, z1).setColor(color.r(), color.g(), color.b(), color.a());
        buffer.addVertex(pose, x2, y2, z2).setColor(color.r(), color.g(), color.b(), color.a());
        buffer.addVertex(pose, x3, y3, z3).setColor(color.r(), color.g(), color.b(), color.a());
    }

    private static void addQuad(VertexConsumer buffer, PoseStack.Pose pose,
                                Quad quad,
                                Vec3 cameraPos,
                                EdenColor color) {
        float x0 = (float) (quad.a().x() - cameraPos.x);
        float y0 = (float) (quad.a().y() - cameraPos.y);
        float z0 = (float) (quad.a().z() - cameraPos.z);
        float x1 = (float) (quad.b().x() - cameraPos.x);
        float y1 = (float) (quad.b().y() - cameraPos.y);
        float z1 = (float) (quad.b().z() - cameraPos.z);
        float x2 = (float) (quad.c().x() - cameraPos.x);
        float y2 = (float) (quad.c().y() - cameraPos.y);
        float z2 = (float) (quad.c().z() - cameraPos.z);
        float x3 = (float) (quad.d().x() - cameraPos.x);
        float y3 = (float) (quad.d().y() - cameraPos.y);
        float z3 = (float) (quad.d().z() - cameraPos.z);
        addQuad(buffer, pose, x0, y0, z0, x1, y1, z1, x2, y2, z2, x3, y3, z3, color);
    }

    static void writeFaceQuads(VertexConsumer buffer,
                               PoseStack.Pose pose,
                               List<Quad> faces,
                               Vec3 cameraPos,
                               EdenColor color) {
        for (Quad face : faces) {
            addQuad(buffer, pose, face, cameraPos, color);
        }
    }
}
