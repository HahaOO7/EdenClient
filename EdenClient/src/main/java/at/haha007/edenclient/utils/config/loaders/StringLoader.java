package at.haha007.edenclient.utils.config.loaders;

import net.minecraft.nbt.StringTag;
import org.jetbrains.annotations.NotNull;

public class StringLoader implements ConfigLoader<StringTag, String> {

    @NotNull
    public StringTag save(@NotNull String value) {
        return StringTag.valueOf(value);
    }

    @NotNull
    public String load(@NotNull StringTag nbtElement) {
        return nbtElement.getAsString();
    }

    @NotNull
    public StringTag parse(@NotNull String s) {
        return StringTag.valueOf(s);
    }
}
