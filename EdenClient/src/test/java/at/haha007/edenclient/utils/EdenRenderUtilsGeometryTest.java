package at.haha007.edenclient.utils;

import at.haha007.edenclient.render.EdenColor;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EdenRenderUtilsGeometryTest {

    @Test
    void areaFillEmits24VerticesCoveringEveryCornerThreeTimes() {
        RecordingVertexConsumer vc = new RecordingVertexConsumer();
        EdenColor color = new EdenColor(1f, 0.5f, 0.25f, 0.4f);

        EdenRenderUtils.writeAreaFillQuads(vc, new PoseStack().last(), 0, 0, 0, 2, 3, 4, color);
        vc.flush();

        assertEquals(24, vc.vertices.size());
        Map<String, Integer> cornerCounts = new HashMap<>();
        for (Vertex v : vc.vertices) {
            cornerCounts.merge(cornerKey(v), 1, Integer::sum);
        }
        assertEquals(8, cornerCounts.size(), "expected the 8 corners of the box");
        cornerCounts.values().forEach(count -> assertEquals(3, count, "each corner belongs to 3 faces"));
    }

    @Test
    void areaFillEmitsCoplanarQuadFaces() {
        RecordingVertexConsumer vc = new RecordingVertexConsumer();
        EdenRenderUtils.writeAreaFillQuads(vc, new PoseStack().last(), 0, 0, 0, 1, 1, 1, EdenColor.WHITE);
        vc.flush();

        for (int i = 0; i < 24; i += 4) {
            assertTrue(isAxisAlignedPlane(vc.vertices.subList(i, i + 4)),
                    "quad starting at vertex " + i + " is not an axis-aligned plane");
        }
    }

    @Test
    void areaFillColorsEveryVertex() {
        RecordingVertexConsumer vc = new RecordingVertexConsumer();
        EdenColor color = new EdenColor(1f, 0.5f, 0.25f, 0.4f);

        EdenRenderUtils.writeAreaFillQuads(vc, new PoseStack().last(), 0, 0, 0, 1, 1, 1, color);
        vc.flush();

        for (Vertex v : vc.vertices) {
            assertEquals(1f, v.r(), 0.01);
            assertEquals(0.5f, v.g(), 0.01);
            assertEquals(0.25f, v.b(), 0.01);
            assertEquals(0.4f, v.a(), 0.01);
        }
    }

    @Test
    void widthLineEmitsSegmentDirectionAsNormalAndWidthPerVertex() {
        RecordingVertexConsumer vc = new RecordingVertexConsumer();
        EdenColor color = new EdenColor(0f, 1f, 0f, 1f);

        EdenRenderUtils.addLine(vc, new PoseStack().last(), 0, 0, 0, 0, 4, 0, color, 2.5f);
        vc.flush();

        assertEquals(2, vc.vertices.size());
        for (Vertex v : vc.vertices) {
            assertEquals(0f, v.r(), 0.01);
            assertEquals(1f, v.g(), 0.01);
            assertEquals(0f, v.b(), 0.01);
            assertEquals(1f, v.a(), 0.01);
            assertEquals(0f, v.nx(), 0.001);
            assertEquals(1f, v.ny(), 0.001);
            assertEquals(0f, v.nz(), 0.001);
            assertEquals(2.5f, v.lineWidth(), 0.001);
        }
    }

    @Test
    void zeroLengthWidthLineStillEmitsTwoFiniteVertices() {
        RecordingVertexConsumer vc = new RecordingVertexConsumer();

        EdenRenderUtils.addLine(vc, new PoseStack().last(), 1, 1, 1, 1, 1, 1, EdenColor.WHITE, 1f);
        vc.flush();

        assertEquals(2, vc.vertices.size());
        for (Vertex v : vc.vertices) {
            assertTrue(Float.isFinite(v.nx()) && Float.isFinite(v.ny()) && Float.isFinite(v.nz()));
        }
    }

    private static String cornerKey(Vertex v) {
        return Math.round(v.x()) + "," + Math.round(v.y()) + "," + Math.round(v.z());
    }

    private static boolean isAxisAlignedPlane(List<Vertex> quad) {
        return quad.stream().map(Vertex::x).distinct().count() == 1
                || quad.stream().map(Vertex::y).distinct().count() == 1
                || quad.stream().map(Vertex::z).distinct().count() == 1;
    }

    private record Vertex(float x, float y, float z,
                          float r, float g, float b, float a,
                          float nx, float ny, float nz, float lineWidth) {
    }

    /**
     * Captures the vertex stream written through the {@link VertexConsumer} contract.
     */
    private static final class RecordingVertexConsumer implements VertexConsumer {
        final List<Vertex> vertices = new ArrayList<>();
        private boolean pending;
        private float x, y, z;
        private int color = 0xFFFFFFFF;
        private float nx, ny, nz;
        private float lineWidth = 1f;

        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            flush();
            this.x = x;
            this.y = y;
            this.z = z;
            pending = true;
            return this;
        }

        @Override
        public VertexConsumer setColor(int r, int g, int b, int a) {
            color = (a << 24) | (r << 16) | (g << 8) | b;
            return this;
        }

        @Override
        public VertexConsumer setColor(int rgba) {
            color = rgba;
            return this;
        }

        @Override
        public VertexConsumer setUv(float u, float v) {
            return this;
        }

        @Override
        public VertexConsumer setUv1(int u, int v) {
            return this;
        }

        @Override
        public VertexConsumer setUv2(int u, int v) {
            return this;
        }

        @Override
        public VertexConsumer setUv3(float u, float v) {
            return this;
        }

        @Override
        public VertexConsumer setNormal(float x, float y, float z) {
            nx = x;
            ny = y;
            nz = z;
            return this;
        }

        @Override
        public VertexConsumer setLineWidth(float lineWidth) {
            this.lineWidth = lineWidth;
            return this;
        }

        void flush() {
            if (!pending) {
                return;
            }
            int a = (color >>> 24) & 0xFF;
            int r = (color >>> 16) & 0xFF;
            int g = (color >>> 8) & 0xFF;
            int b = color & 0xFF;
            vertices.add(new Vertex(x, y, z, r / 255f, g / 255f, b / 255f, a / 255f, nx, ny, nz, lineWidth));
            pending = false;
        }
    }
}