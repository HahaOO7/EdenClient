package at.haha007.edenclient.utils.config.loaders;

import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.NotNull;

public interface ConfigLoader<C extends Tag, V> {
    @NotNull
    C save(@NotNull V value);

    @NotNull
    V load(@NotNull C nbtElement);

    @NotNull
    C parse(@NotNull String s);

}
