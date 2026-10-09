package at.haha007.edenclient.render;

import org.jspecify.annotations.NonNull;

public record EdenColor(float r, float g, float b, float a) {
    public static final EdenColor WHITE = new EdenColor(1f, 1f, 1f, 1f);

    public EdenColor(float r, float g, float b, float a) {
        this.r = clamp(r);
        this.g = clamp(g);
        this.b = clamp(b);
        this.a = clamp(a);
    }

    public EdenColor(float r, float g, float b) {
        this(r, g, b, 1f);
    }

    public EdenColor withAlpha(float alpha) {
        return new EdenColor(r, g, b, alpha);
    }

    public static EdenColor fromColor(int argb) {
        float a = ((argb >> 24) & 0xFF) / 255f;
        float r = ((argb >> 16) & 0xFF) / 255f;
        float g = ((argb >> 8) & 0xFF) / 255f;
        float b = (argb & 0xFF) / 255f;
        return new EdenColor(r, g, b, a);
    }

    private static float clamp(float v) {
        return Math.clamp(v, 0f, 1f);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof EdenColor(float r1, float g1, float b1, float a1))) {
            return false;
        }
        return Float.compare(r, r1) == 0
                && Float.compare(g, g1) == 0
                && Float.compare(b, b1) == 0
                && Float.compare(a, a1) == 0;
    }

    @Override
    @NonNull
    public String toString() {
        return "EdenColor[r=" + r + ", g=" + g + ", b=" + b + ", a=" + a + "]";
    }
}
