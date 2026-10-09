package at.haha007.edenclient.render;

import at.haha007.edenclient.mixin.RenderTypeInvoker;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;

/** Render types used by the submit-based line renderer. */
public final class EdenRenderTypes {
    private EdenRenderTypes() {
    }

    public static final RenderType LINES_NO_DEPTH_NO_CULL =
            RenderTypeInvoker.edenclient$create(
                    "edenclient_lines_no_depth",
                    RenderSetup.builder(EdenPipelines.LINES_NO_DEPTH_NO_CULL).createRenderSetup());
}
