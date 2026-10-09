package at.haha007.edenclient.render;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EdenColorTest {
    @Test
    void fromColorDecodesOpaqueWhite() {
        EdenColor c = EdenColor.fromColor(0xFFFFFFFF);
        assertEquals(1f, c.a(), 1e-6);
        assertEquals(1f, c.r(), 1e-6);
        assertEquals(1f, c.g(), 1e-6);
        assertEquals(1f, c.b(), 1e-6);
    }

    @Test
    void fromColorDecodesChannels() {
        EdenColor c = EdenColor.fromColor(0x80102030);
        assertEquals(0x80 / 255f, c.a(), 1e-6);
        assertEquals(0x10 / 255f, c.r(), 1e-6);
        assertEquals(0x20 / 255f, c.g(), 1e-6);
        assertEquals(0x30 / 255f, c.b(), 1e-6);
    }

    @Test
    void fromColorDecodesTransparentBlack() {
        EdenColor c = EdenColor.fromColor(0x00000000);
        assertEquals(0f, c.a(), 1e-6);
        assertEquals(0f, c.r(), 1e-6);
    }

    @Test
    void componentsAreClamped() {
        EdenColor c = new EdenColor(2f, -1f, 0.5f, 5f);
        assertEquals(1f, c.r(), 1e-6);
        assertEquals(0f, c.g(), 1e-6);
        assertEquals(0.5f, c.b(), 1e-6);
        assertEquals(1f, c.a(), 1e-6);
    }

    @Test
    void whiteConstantIsOpaqueWhite() {
        assertEquals(EdenColor.fromColor(0xFFFFFFFF), EdenColor.WHITE);
    }

    @Test
    void withAlphaReplacesAlphaOnly() {
        EdenColor c = EdenColor.WHITE.withAlpha(0.25f);
        assertEquals(1f, c.r(), 1e-6);
        assertEquals(0.25f, c.a(), 1e-6);
    }
}
