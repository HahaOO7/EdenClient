package at.haha007.edenclient.utils.config.loaders;

import net.minecraft.nbt.StringTag;

public class StringLoader implements ConfigLoader<StringTag, String> {

    public StringTag save(String value) {
        return StringTag.valueOf(value);
    }

    public String load(StringTag nbtElement) {
        return nbtElement.getAsString();
    }

    public StringTag parse(String s) {
        return StringTag.valueOf(s);
    }
}
