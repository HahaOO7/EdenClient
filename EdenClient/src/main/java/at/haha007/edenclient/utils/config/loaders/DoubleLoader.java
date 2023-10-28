package at.haha007.edenclient.utils.config.loaders;

import net.minecraft.nbt.DoubleTag;

public class DoubleLoader implements ConfigLoader<DoubleTag, Double> {

    public DoubleTag save(Double value) {
        return DoubleTag.valueOf(value);
    }

    public Double load(DoubleTag nbtElement) {
        return nbtElement.getAsDouble();
    }

    public DoubleTag parse(String s) {
        return DoubleTag.valueOf(s.isEmpty() ? 0 : Double.parseDouble(s));
    }
}
