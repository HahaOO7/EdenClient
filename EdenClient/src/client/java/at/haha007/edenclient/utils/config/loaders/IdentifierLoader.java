package at.haha007.edenclient.utils.config.loaders;

import net.minecraft.nbt.StringTag;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

public class IdentifierLoader implements ConfigLoader<StringTag, Identifier> {

    @Override
    @NonNull
    public StringTag save(@NonNull Identifier value) {
        return StringTag.valueOf(value.toString());
    }

    @Override
    @NonNull
    public Identifier load(@NonNull StringTag nbtElement) {
        return Identifier.parse(nbtElement.asString().orElseThrow());
    }

    @Override
    @NonNull
    public StringTag parse(@NotNull String s) {
        return StringTag.valueOf(s);
    }
}
