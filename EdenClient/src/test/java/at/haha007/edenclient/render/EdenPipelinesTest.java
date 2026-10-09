package at.haha007.edenclient.render;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.renderpearl.api.pipeline.CompareOp;
import com.mojang.renderpearl.api.pipeline.DepthStencilState;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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
}
