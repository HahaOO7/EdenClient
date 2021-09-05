package at.haha007.edenclient.utils.config.loaders;

import at.haha007.edenclient.utils.config.PerWorldConfig;
import at.haha007.edenclient.utils.config.wrappers.StringVec3iMap;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.Vec3i;

public class StringVec3iMapLoader implements ConfigLoader<NbtCompound, StringVec3iMap> {

    public NbtCompound save(Object value) {
        NbtCompound tag = new NbtCompound();
        cast(value).forEach((k, v) -> tag.put(k, PerWorldConfig.get().toNbt(v)));
        return tag;
    }

    public StringVec3iMap load(NbtCompound tag) {
        StringVec3iMap list = new StringVec3iMap();
        for (String key : tag.getKeys()) {
            list.put(key, PerWorldConfig.get().toObject(tag.get(key), Vec3i.class));
        }
        return list;
    }

    public NbtCompound parse(String s) {
        return new NbtCompound();
    }
}
