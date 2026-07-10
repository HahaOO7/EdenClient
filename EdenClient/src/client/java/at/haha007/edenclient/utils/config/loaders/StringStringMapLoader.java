package at.haha007.edenclient.utils.config.loaders;

import at.haha007.edenclient.utils.config.wrappers.StringStringMap;
import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.NotNull;

public class StringStringMapLoader implements ConfigLoader<CompoundTag, StringStringMap> {

    @NotNull
    public CompoundTag save(@NotNull StringStringMap value) {
        CompoundTag tag = new CompoundTag();
        value.forEach(tag::putString);
        return tag;
    }

    @NotNull
    public StringStringMap load(@NotNull CompoundTag tag) {
        StringStringMap list = new StringStringMap();
        for (String key : tag.keySet()) {
            list.put(key, tag.getString(key).orElseThrow());
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
