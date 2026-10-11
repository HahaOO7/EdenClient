package at.haha007.edenclient.render;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.renderpearl.api.pipeline.BlendFunction;
import com.mojang.renderpearl.api.pipeline.CompareOp;
import com.mojang.renderpearl.api.pipeline.DepthStencilState;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EdenPipelinesTest {
    @Test
    void linePipelineUsesDebugLinesTopology() {
        assertEquals(PrimitiveTopology.DEBUG_LINES,
                EdenPipelines.LINES_NO_DEPTH_NO_CULL.getPrimitiveTopology());
    }

    @Test
    void linePipelineUsesPositionColorFormat() {
        assertEquals(DefaultVertexFormat.POSITION_COLOR,
                EdenPipelines.LINES_NO_DEPTH_NO_CULL.getVertexFormatBinding(0));
    }

    @Test
    void linePipelineDrawsThroughWalls() {
        DepthStencilState depth = EdenPipelines.LINES_NO_DEPTH_NO_CULL.getDepthStencilState();
        assertNotNull(depth);
        assertEquals(CompareOp.ALWAYS_PASS, depth.depthTest());
        assertFalse(depth.writeDepth());
        assertFalse(EdenPipelines.LINES_NO_DEPTH_NO_CULL.isCull());
    }

    @Test
    void debugLinesKeepsOneIndexPerVertexSoSingleSegmentsDraw() {
        // A lone 2-vertex segment must produce 2 indices (one GL line), unlike LINES.
        assertEquals(2, PrimitiveTopology.DEBUG_LINES.indexCount(2));
        assertEquals(0, PrimitiveTopology.LINES.indexCount(2));
    }

    @Test
    void areaFillPipelineUsesQuadsTopology() {
        assertEquals(PrimitiveTopology.QUADS,
                EdenPipelines.AREA_FILL_DEPTH_TESTED.getPrimitiveTopology());
    }

    @Test
    void areaFillPipelineUsesPositionColorFormat() {
        assertEquals(DefaultVertexFormat.POSITION_COLOR,
                EdenPipelines.AREA_FILL_DEPTH_TESTED.getVertexFormatBinding(0));
    }

    @Test
    void areaFillPipelineIsDepthTestedAndWritesNoDepth() {
        DepthStencilState depth = EdenPipelines.AREA_FILL_DEPTH_TESTED.getDepthStencilState();
        assertNotNull(depth);
        // This renderer uses a reversed-Z depth buffer (larger value = closer to camera),
        // so the "closer or equal" test is GREATER_THAN_OR_EQUAL, matching vanilla's DEFAULT.
        assertEquals(CompareOp.GREATER_THAN_OR_EQUAL, depth.depthTest());
        assertFalse(depth.writeDepth());
        assertFalse(EdenPipelines.AREA_FILL_DEPTH_TESTED.isCull());
    }

    @Test
    void areaFillPipelineBlendsTranslucent() {
        assertTrue(EdenPipelines.AREA_FILL_DEPTH_TESTED.getColorTargetStates().stream()
                .anyMatch(state -> state.blendFunction().isPresent()
                        && state.blendFunction().get() == BlendFunction.TRANSLUCENT));
    }

    @Test
    void widthLinePipelineUsesLineWidthFormat() {
        assertEquals(DefaultVertexFormat.POSITION_COLOR_NORMAL_LINE_WIDTH,
                EdenPipelines.LINES_WIDTH_NO_DEPTH_NO_CULL.getVertexFormatBinding(0));
        assertEquals(PrimitiveTopology.LINES,
                EdenPipelines.LINES_WIDTH_NO_DEPTH_NO_CULL.getPrimitiveTopology());
    }

    @Test
    void widthLineNoDepthPipelineDrawsThroughWalls() {
        DepthStencilState depth = EdenPipelines.LINES_WIDTH_NO_DEPTH_NO_CULL.getDepthStencilState();
        assertNotNull(depth);
        assertEquals(CompareOp.ALWAYS_PASS, depth.depthTest());
        assertFalse(depth.writeDepth());
        assertFalse(EdenPipelines.LINES_WIDTH_NO_DEPTH_NO_CULL.isCull());
    }

    @Test
    void widthLineDepthTestedPipelineUsesReversedZDepthTest() {
        DepthStencilState depth = EdenPipelines.LINES_WIDTH_DEPTH_TESTED.getDepthStencilState();
        assertNotNull(depth);
        assertEquals(CompareOp.GREATER_THAN_OR_EQUAL, depth.depthTest());
        assertFalse(depth.writeDepth());
        assertEquals(DefaultVertexFormat.POSITION_COLOR_NORMAL_LINE_WIDTH,
                EdenPipelines.LINES_WIDTH_DEPTH_TESTED.getVertexFormatBinding(0));
    }

    @Test
    void widthLinePipelinesUseVanillaLineExpansionShader() {
        Identifier lineShader = Identifier.fromNamespaceAndPath("minecraft", "core/rendertype_lines");
        assertTrue(EdenPipelines.LINES_WIDTH_NO_DEPTH_NO_CULL.getShaders().containsValue(lineShader));
        assertTrue(EdenPipelines.LINES_WIDTH_DEPTH_TESTED.getShaders().containsValue(lineShader));
    }

    @Test
    void areaFillNoDepthPipelineUsesQuadsAndPositionColor() {
        assertEquals(PrimitiveTopology.QUADS,
                EdenPipelines.AREA_FILL_NO_DEPTH_NO_CULL.getPrimitiveTopology());
        assertEquals(DefaultVertexFormat.POSITION_COLOR,
                EdenPipelines.AREA_FILL_NO_DEPTH_NO_CULL.getVertexFormatBinding(0));
    }

    @Test
    void areaFillNoDepthPipelineDrawsThroughWalls() {
        DepthStencilState depth = EdenPipelines.AREA_FILL_NO_DEPTH_NO_CULL.getDepthStencilState();
        assertNotNull(depth);
        assertEquals(CompareOp.ALWAYS_PASS, depth.depthTest());
        assertFalse(depth.writeDepth());
        assertFalse(EdenPipelines.AREA_FILL_NO_DEPTH_NO_CULL.isCull());
    }

    @Test
    void areaFillNoDepthPipelineBlendsTranslucent() {
        assertTrue(EdenPipelines.AREA_FILL_NO_DEPTH_NO_CULL.getColorTargetStates().stream()
                .anyMatch(state -> state.blendFunction().isPresent()
                        && state.blendFunction().get() == BlendFunction.TRANSLUCENT));
    }
}
