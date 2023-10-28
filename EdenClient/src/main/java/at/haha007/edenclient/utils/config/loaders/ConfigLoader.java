package at.haha007.edenclient.utils.config.loaders;

import net.minecraft.nbt.Tag;

public interface ConfigLoader<C extends Tag, V> {
    C save(V value);

    V load(C nbtElement);

    C parse(String s);

}
