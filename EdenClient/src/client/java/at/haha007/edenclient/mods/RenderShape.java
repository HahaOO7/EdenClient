package at.haha007.edenclient.mods;

import at.haha007.edenclient.annotations.Mod;
import at.haha007.edenclient.callbacks.GameRenderCallback;
import at.haha007.edenclient.render.FaceRenderer;

@Mod
public class RenderShape {
    private final FaceRenderer cubeRenderer = new FaceRenderer();

    public RenderShape() {
        GameRenderCallback.EVENT.register(this::render, getClass());
    }

    private void render(float v) {
    }
}
