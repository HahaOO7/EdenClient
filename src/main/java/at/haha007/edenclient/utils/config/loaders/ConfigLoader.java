package at.haha007.edenclient.utils.config.loaders;

import net.minecraft.nbt.NbtElement;

public interface ConfigLoader<C extends NbtElement, V> {
    C save(Object value);

    V load(C nbtElement);

    C parse(String s);

    @SuppressWarnings("unchecked")
    default V cast(Object obj) {
        return (V) obj;
    }
}
