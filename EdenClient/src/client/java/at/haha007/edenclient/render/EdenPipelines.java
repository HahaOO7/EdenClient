package at.haha007.edenclient.render;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.renderpearl.api.pipeline.BlendFunction;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.CompareOp;
import com.mojang.renderpearl.api.pipeline.DepthStencilState;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.resources.Identifier;

/**
 * EdenClient-owned render pipelines.
 */
public final class EdenPipelines {
    private EdenPipelines() {
    }

    private static final Identifier POSITION_COLOR_SHADER =
            Identifier.fromNamespaceAndPath("minecraft", "core/position_color");

    private static final Identifier LINES_WIDTH_SHADER =
            Identifier.fromNamespaceAndPath("minecraft", "core/rendertype_lines");

    /** Thin, vertex-colored lines drawn through terrain (depth test always passes). */
    public static final RenderPipeline LINES_NO_DEPTH_NO_CULL =
            RenderPipeline.builder()
                    .withVertexShader(POSITION_COLOR_SHADER)
                    .withFragmentShader(POSITION_COLOR_SHADER)
                    .withBindGroupLayout(BindGroupLayouts.DYNAMIC_TRANSFORMS)
                    .withBindGroupLayout(BindGroupLayouts.PROJECTION)
                    .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR)
                    .withPrimitiveTopology(PrimitiveTopology.DEBUG_LINES)
                    .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
                    .withCull(false)
                    .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
                    .withLocation(Identifier.fromNamespaceAndPath("edenclient", "pipeline/lines/no_depth/no_cull"))
                    .build();

    /** Translucent, depth-tested quads filling a box area (occluded by terrain). */
    public static final RenderPipeline AREA_FILL_DEPTH_TESTED =
            buildFill(CompareOp.LESS_THAN, "pipeline/fill/depth_tested");

    /** Translucent quads filling a box area drawn through terrain (depth test always passes). */
    public static final RenderPipeline AREA_FILL_NO_DEPTH_NO_CULL =
            buildFill(CompareOp.ALWAYS_PASS, "pipeline/fill/no_depth/no_cull");

    /** Width-capable lines drawn through terrain (depth test always passes). */
    public static final RenderPipeline LINES_WIDTH_NO_DEPTH_NO_CULL =
            buildWidthLines(CompareOp.ALWAYS_PASS, "pipeline/lines/width/no_depth/no_cull");

    /** Width-capable lines occluded by terrain. */
    public static final RenderPipeline LINES_WIDTH_DEPTH_TESTED =
            buildWidthLines(CompareOp.LESS_THAN, "pipeline/lines/width/depth_tested");

    private static RenderPipeline buildFill(CompareOp depthCompare, String location) {
        return RenderPipeline.builder()
                .withVertexShader(POSITION_COLOR_SHADER)
                .withFragmentShader(POSITION_COLOR_SHADER)
                .withBindGroupLayout(BindGroupLayouts.DYNAMIC_TRANSFORMS)
                .withBindGroupLayout(BindGroupLayouts.PROJECTION)
                .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR)
                .withPrimitiveTopology(PrimitiveTopology.QUADS)
                .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
                .withCull(false)
                .withDepthStencilState(new DepthStencilState(depthCompare, false))
                .withLocation(Identifier.fromNamespaceAndPath("edenclient", location))
                .build();
    }

    private static RenderPipeline buildWidthLines(CompareOp depthCompare, String location) {
        return RenderPipeline.builder()
                .withVertexShader(LINES_WIDTH_SHADER)
                .withFragmentShader(LINES_WIDTH_SHADER)
                .withBindGroupLayout(BindGroupLayouts.GLOBALS)
                .withBindGroupLayout(BindGroupLayouts.PROJECTION)
                .withBindGroupLayout(BindGroupLayouts.DYNAMIC_TRANSFORMS)
                .withBindGroupLayout(BindGroupLayouts.FOG)
                .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR_NORMAL_LINE_WIDTH)
                .withPrimitiveTopology(PrimitiveTopology.LINES)
                .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
                .withCull(false)
                .withDepthStencilState(new DepthStencilState(depthCompare, false))
                .withLocation(Identifier.fromNamespaceAndPath("edenclient", location))
                .build();
    }
}
