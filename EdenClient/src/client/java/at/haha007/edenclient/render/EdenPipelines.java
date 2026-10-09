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
}
