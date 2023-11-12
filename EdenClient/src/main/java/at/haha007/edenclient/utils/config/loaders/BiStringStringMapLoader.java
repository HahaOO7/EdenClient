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
