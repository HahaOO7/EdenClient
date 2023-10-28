package at.haha007.edenclient.utils.config.loaders;

import net.minecraft.nbt.FloatTag;

public class FloatLoader implements ConfigLoader<FloatTag, Float> {

    public FloatTag save(Float value) {
        return FloatTag.valueOf(value);
    }

    public Float load(FloatTag nbtElement) {
        return nbtElement.getAsFloat();
    }

    public FloatTag parse(String s) {
        return FloatTag.valueOf(s.isEmpty() ? 0 : Float.parseFloat(s));
    }
}
