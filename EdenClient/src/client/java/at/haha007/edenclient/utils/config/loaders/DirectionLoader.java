package at.haha007.edenclient.utils.config.loaders;

import net.minecraft.core.Direction;
import net.minecraft.nbt.StringTag;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;

public class DirectionLoader implements ConfigLoader<StringTag, Direction> {
    @Override
    @NotNull
    public StringTag save(@NotNull Direction value) {
        return StringTag.valueOf(value.getName());
    }

    @Override
    @NotNull
    public Direction load(@NotNull StringTag nbtElement) {
        return Optional.ofNullable(Direction.byName(nbtElement.asString().orElseThrow())).orElse(Direction.DOWN);
    }

    @Override
    @NotNull
    public StringTag parse(@NotNull String s) {
        return StringTag.valueOf(s);
    }
}
