package at.haha007.edenclient.utils.config.loaders;

import at.haha007.edenclient.utils.config.wrappers.BiStringStringMap;
import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.NotNull;

public class BiStringStringMapLoader implements ConfigLoader<CompoundTag, BiStringStringMap> {

    @NotNull
    public CompoundTag save(@NotNull BiStringStringMap value) {
        CompoundTag tag = new CompoundTag();
        value.forEach(tag::putString);
        return tag;
    }

    @NotNull
    public BiStringStringMap load(@NotNull CompoundTag tag) {
        BiStringStringMap list = new BiStringStringMap();
        for (String key : tag.getAllKeys()) {
            list.put(key, tag.getString(key));
        }
        return list;
    }

    @NotNull
    public CompoundTag parse(@NotNull String inputString) {
        CompoundTag tag = new CompoundTag();
        if (inputString.isEmpty())
            return tag;

        String[] entries = inputString.split(";");
        for (int i = 0; i < entries.length; i += 2) {
            String key = entries[i];
            String value = entries[i + 1];
            tag.putString(key, value);
        }

        return tag;
    }
}
