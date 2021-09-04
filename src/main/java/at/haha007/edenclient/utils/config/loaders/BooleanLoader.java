package at.haha007.edenclient.utils.config.loaders;

import net.minecraft.nbt.NbtByte;

public class BooleanLoader implements ConfigLoader<NbtByte, Boolean> {
    public NbtByte save(Object value) {
        return NbtByte.of(cast(value));
    }

    public Boolean load(NbtByte value) {
        return value.byteValue() != 0;
    }

    public NbtByte parse(String s) {
        return NbtByte.of(Boolean.parseBoolean(s));
    }
}
