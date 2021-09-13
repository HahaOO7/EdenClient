package at.haha007.edenclient.utils.config.loaders;

import net.minecraft.nbt.NbtFloat;

public class FloatLoader implements ConfigLoader<NbtFloat, Float> {

    public NbtFloat save(Object value) {
        return NbtFloat.of(cast(value));
    }

    public Float load(NbtFloat nbtElement) {
        return nbtElement.floatValue();
    }

    public NbtFloat parse(String s) {
        return NbtFloat.of(s.isEmpty() ? 0 : Float.parseFloat(s));
    }
}
