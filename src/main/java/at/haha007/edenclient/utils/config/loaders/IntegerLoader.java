package at.haha007.edenclient.utils.config.loaders;

import net.minecraft.nbt.NbtInt;

public class IntegerLoader implements ConfigLoader<NbtInt, Integer> {
    public NbtInt save(Object value) {
        return NbtInt.of(cast(value));
    }

    public Integer load(NbtInt value) {
        return value.intValue();
    }

    public NbtInt parse(String s) {
        return NbtInt.of(Integer.parseInt(s));
    }
}
