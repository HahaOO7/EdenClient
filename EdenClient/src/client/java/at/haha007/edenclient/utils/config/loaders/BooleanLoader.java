package at.haha007.edenclient.utils.config.loaders;

import net.minecraft.nbt.ByteTag;
import org.jetbrains.annotations.NotNull;

public class BooleanLoader implements ConfigLoader<ByteTag, Boolean> {
    @NotNull
    public ByteTag save(@NotNull Boolean value) {
        return ByteTag.valueOf(value);
    }

    @NotNull
    public Boolean load(@NotNull ByteTag value) {
        return value.asBoolean().orElseThrow();
    }

    @NotNull
    public ByteTag parse(@NotNull String s) {
        if (s.isEmpty()) return ByteTag.valueOf(false);
        return ByteTag.valueOf(Boolean.parseBoolean(s));
    }
}
