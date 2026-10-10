package at.haha007.edenclient.render;

import at.haha007.edenclient.utils.EdenRenderUtils;
import net.minecraft.world.phys.AABB;

import java.util.HashSet;
import java.util.Set;

public class CubeRenderer {
    private final Set<AABB> cubes = new HashSet<>();
    private final Settings settings = new Settings();

    public void render(float v) {
        Settings s = settings;
        for (AABB box : cubes) {
            if (s.facesEnabled()) {
                EdenRenderUtils.drawAreaFill(
                        box.getMinPosition(), box.getMaxPosition(), s.faceColor(), s.faceDepth());
            }
            if (s.edgesEnabled()) {
                EdenRenderUtils.drawAreaOutline(
                        box.getMinPosition(), box.getMaxPosition(), s.edgeWidth(), s.edgeColor(), s.edgeDepth());
            }
        }
    }

    public void add(AABB box) {
        cubes.add(box);
    }

    public void clear() {
        cubes.clear();
    }

    public Settings settings() {
        return settings;
    }

    public static final class Settings {
        private boolean facesEnabled = false;
        private EdenColor faceColor = new EdenColor(1f, 1f, 1f, 0.4f);
        private EdenRenderUtils.DepthTest faceDepth = EdenRenderUtils.DepthTest.NO_DEPTH;
        private boolean edgesEnabled = true;
        private EdenColor edgeColor = new EdenColor(1f, 0f, 1f, 1f);
        private float edgeWidth = 1f;
        private EdenRenderUtils.DepthTest edgeDepth = EdenRenderUtils.DepthTest.NO_DEPTH;

        public boolean facesEnabled() {
            return facesEnabled;
        }

        public Settings facesEnabled(boolean facesEnabled) {
            this.facesEnabled = facesEnabled;
            return this;
        }

        public EdenColor faceColor() {
            return faceColor;
        }

        public Settings faceColor(EdenColor faceColor) {
            this.faceColor = faceColor;
            return this;
        }

        public EdenRenderUtils.DepthTest faceDepth() {
            return faceDepth;
        }

        public Settings faceDepth(EdenRenderUtils.DepthTest faceDepth) {
            this.faceDepth = faceDepth;
            return this;
        }

        public boolean edgesEnabled() {
            return edgesEnabled;
        }

        public Settings edgesEnabled(boolean edgesEnabled) {
            this.edgesEnabled = edgesEnabled;
            return this;
        }

        public EdenColor edgeColor() {
            return edgeColor;
        }

        public Settings edgeColor(EdenColor edgeColor) {
            this.edgeColor = edgeColor;
            return this;
        }

        public float edgeWidth() {
            return edgeWidth;
        }

        public Settings edgeWidth(float edgeWidth) {
            this.edgeWidth = edgeWidth;
            return this;
        }

        public EdenRenderUtils.DepthTest edgeDepth() {
            return edgeDepth;
        }

        public Settings edgeDepth(EdenRenderUtils.DepthTest edgeDepth) {
            this.edgeDepth = edgeDepth;
            return this;
        }
    }
}