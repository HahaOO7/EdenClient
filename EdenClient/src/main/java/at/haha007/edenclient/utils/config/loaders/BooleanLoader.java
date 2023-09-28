package at.haha007.edenclient.utils.config.loaders;

import net.minecraft.nbt.ByteTag;

public class BooleanLoader implements ConfigLoader<ByteTag, Boolean> {
    public ByteTag save(Object value) {
        return ByteTag.valueOf(cast(value));
    }

    public Boolean load(ByteTag value) {
        return value.getAsByte() != 0;
    }

    public ByteTag parse(String s) {
        if (s.isEmpty()) return ByteTag.valueOf(false);
        return ByteTag.valueOf(Boolean.parseBoolean(s));
    }
}
