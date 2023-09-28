package at.haha007.edenclient.utils.config.loaders;

import net.minecraft.nbt.DoubleTag;

public class DoubleLoader implements ConfigLoader<DoubleTag, Double> {

    public DoubleTag save(Object value) {
        return DoubleTag.valueOf(cast(value));
    }

    public Double load(DoubleTag nbtElement) {
        return nbtElement.getAsDouble();
    }

    public DoubleTag parse(String s) {
        return DoubleTag.valueOf(s.isEmpty() ? 0 : Double.parseDouble(s));
    }
}
