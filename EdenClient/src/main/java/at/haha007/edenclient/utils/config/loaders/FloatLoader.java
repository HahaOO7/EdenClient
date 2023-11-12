package at.haha007.edenclient.utils.config.loaders;

import net.minecraft.nbt.FloatTag;
import org.jetbrains.annotations.NotNull;

public class FloatLoader implements ConfigLoader<FloatTag, Float> {
    @NotNull
    public FloatTag save(@NotNull Float value) {
        return FloatTag.valueOf(value);
    }

    @NotNull
    public Float load(@NotNull FloatTag nbtElement) {
        return nbtElement.getAsFloat();
    }

    @NotNull
    public FloatTag parse(@NotNull String s) {
        return FloatTag.valueOf(s.isEmpty() ? 0 : Float.parseFloat(s));
    }
}
