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
        for (String key : tag.getAllKeys()) {
            list.put(key, tag.getString(key));
        }
        return list;
    }

    @NotNull
    public CompoundTag parse(@NotNull String s) {
        CompoundTag tag = new CompoundTag();
        if (s.isEmpty()) return tag;
        String[] a = s.split(";");
        for (int i = 0; i < a.length; i++) {
            String k = a[i];
            i++;
            tag.putString(k, a[i]);
        }
        return tag;
    }
}
