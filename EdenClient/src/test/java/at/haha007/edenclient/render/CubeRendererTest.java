package at.haha007.edenclient.render;

import at.haha007.edenclient.utils.EdenRenderUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CubeRendererTest {

    @Test
    void settingsDefaultToCurrentCubeLook() {
        CubeRenderer.Settings settings = new CubeRenderer().settings();

        // Faces: currently the renderer draws no faces at all.
        assertFalse(settings.facesEnabled());
        assertEquals(1f, settings.faceColor().r(), 0.001);
        assertEquals(1f, settings.faceColor().g(), 0.001);
        assertEquals(1f, settings.faceColor().b(), 0.001);
        assertEquals(0.4f, settings.faceColor().a(), 0.001);
        assertEquals(EdenRenderUtils.DepthTest.NO_DEPTH, settings.faceDepth());

        // Edges: currently a magenta through-wall outline.
        assertTrue(settings.edgesEnabled());
        assertEquals(1f, settings.edgeColor().r(), 0.001);
        assertEquals(0f, settings.edgeColor().g(), 0.001);
        assertEquals(1f, settings.edgeColor().b(), 0.001);
        assertEquals(1f, settings.edgeColor().a(), 0.001);
        assertEquals(1f, settings.edgeWidth(), 0.001);
        assertEquals(EdenRenderUtils.DepthTest.NO_DEPTH, settings.edgeDepth());
    }

    @Test
    void settingsSettersRoundTrip() {
        CubeRenderer.Settings settings = new CubeRenderer.Settings();
        EdenColor red = new EdenColor(1f, 0f, 0f, 0.5f);

        settings.facesEnabled(true);
        settings.faceColor(red);
        settings.faceDepth(EdenRenderUtils.DepthTest.DEPTH_TESTED);
        settings.edgesEnabled(false);
        settings.edgeColor(red);
        settings.edgeWidth(4f);
        settings.edgeDepth(EdenRenderUtils.DepthTest.DEPTH_TESTED);

        assertTrue(settings.facesEnabled());
        assertSame(red, settings.faceColor());
        assertEquals(EdenRenderUtils.DepthTest.DEPTH_TESTED, settings.faceDepth());
        assertFalse(settings.edgesEnabled());
        assertSame(red, settings.edgeColor());
        assertEquals(4f, settings.edgeWidth(), 0.001);
        assertEquals(EdenRenderUtils.DepthTest.DEPTH_TESTED, settings.edgeDepth());
    }
}