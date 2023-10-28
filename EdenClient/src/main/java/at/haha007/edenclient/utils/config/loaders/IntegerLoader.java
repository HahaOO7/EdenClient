package at.haha007.edenclient.utils.config.loaders;

import net.minecraft.nbt.IntTag;

public class IntegerLoader implements ConfigLoader<IntTag, Integer> {

    public IntTag save(Integer value) {
        return IntTag.valueOf(value);
    }

    public Integer load(IntTag value) {
        return value.getAsInt();
    }

    public IntTag parse(String s) {
        if (s.isEmpty()) return IntTag.valueOf(0);
        return IntTag.valueOf(Integer.parseInt(s));
    }
}
