package at.haha007.edenclient.utils.config.loaders;

import net.minecraft.nbt.IntTag;
import org.jetbrains.annotations.NotNull;

public class IntegerLoader implements ConfigLoader<IntTag, Integer> {
    @NotNull
    public IntTag save(@NotNull Integer value) {
        return IntTag.valueOf(value);
    }

    @NotNull
    public Integer load(@NotNull IntTag value) {
        return value.asInt().orElseThrow();
    }

    @NotNull
    public IntTag parse(@NotNull String s) {
        if (s.isEmpty()) return IntTag.valueOf(0);
        return IntTag.valueOf(Integer.parseInt(s));
    }
}
