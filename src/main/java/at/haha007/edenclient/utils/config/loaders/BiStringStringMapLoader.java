package at.haha007.edenclient.utils.config.loaders;

import at.haha007.edenclient.utils.config.wrappers.BiStringStringMap;
import net.minecraft.nbt.CompoundTag;

public class BiStringStringMapLoader implements ConfigLoader<CompoundTag, BiStringStringMap> {

    public CompoundTag save(Object value) {
        CompoundTag tag = new CompoundTag();
        cast(value).forEach(tag::putString);
        return tag;
    }

    public BiStringStringMap load(CompoundTag tag) {
        BiStringStringMap list = new BiStringStringMap();
        for (String key : tag.getAllKeys()) {
            list.put(key, tag.getString(key));
        }
        return list;
    }

    public CompoundTag parse(String s) {
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
