package at.haha007.edenclient.utils.config.loaders;

import net.minecraft.nbt.NbtDouble;

public class DoubleLoader implements ConfigLoader<NbtDouble, Double> {

    public NbtDouble save(Object value) {
        return NbtDouble.of(cast(value));
    }

    public Double load(NbtDouble nbtElement) {
        return nbtElement.doubleValue();
    }

    public NbtDouble parse(String s) {
        return NbtDouble.of(s.isEmpty() ? 0 : Double.parseDouble(s));
    }
}
