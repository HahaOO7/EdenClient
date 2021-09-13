package at.haha007.edenclient.utils.config.loaders;

import net.minecraft.nbt.NbtString;

public class StringLoader implements ConfigLoader<NbtString, String> {

    public NbtString save(Object value) {
        return NbtString.of(cast(value));
    }

    public String load(NbtString nbtElement) {
        return nbtElement.asString();
    }

    public NbtString parse(String s) {
        return NbtString.of(s);
    }
}
