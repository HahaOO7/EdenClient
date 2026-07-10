package at.haha007.edenclient.utils.config.loaders;

import net.minecraft.nbt.DoubleTag;
import org.jetbrains.annotations.NotNull;

public class DoubleLoader implements ConfigLoader<DoubleTag, Double> {

    @NotNull
    public DoubleTag save(@NotNull Double value) {
        return DoubleTag.valueOf(value);
    }

    @NotNull
    public Double load(@NotNull DoubleTag nbtElement) {
        return nbtElement.asDouble().orElseThrow();
    }

    @NotNull
    public DoubleTag parse(@NotNull String s) {
        return DoubleTag.valueOf(s.isEmpty() ? 0 : Double.parseDouble(s));
    }
}
