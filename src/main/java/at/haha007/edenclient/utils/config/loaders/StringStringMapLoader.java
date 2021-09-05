package at.haha007.edenclient.utils.config.loaders;

import at.haha007.edenclient.utils.config.wrappers.StringStringMap;
import net.minecraft.nbt.NbtCompound;

public class StringStringMapLoader implements ConfigLoader<NbtCompound, StringStringMap> {

    public NbtCompound save(Object value) {
        NbtCompound tag = new NbtCompound();
        cast(value).forEach(tag::putString);
        return tag;
    }

    public StringStringMap load(NbtCompound tag) {
        StringStringMap list = new StringStringMap();
        for (String key : tag.getKeys()) {
            list.put(key, tag.getString(key));
        }
        return list;
    }

    public NbtCompound parse(String s) {
        NbtCompound tag = new NbtCompound();
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
