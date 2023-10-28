package at.haha007.edenclient.utils.config.loaders;

import at.haha007.edenclient.utils.config.PerWorldConfig;
import at.haha007.edenclient.utils.config.wrappers.StringVec3iMap;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;

public class StringVec3iMapLoader implements ConfigLoader<CompoundTag, StringVec3iMap> {

    public CompoundTag save(StringVec3iMap value) {
        CompoundTag tag = new CompoundTag();
        value.forEach((k, v) -> tag.put(k, PerWorldConfig.get().toNbt(v)));
        return tag;
    }

    public StringVec3iMap load(CompoundTag tag) {
        StringVec3iMap list = new StringVec3iMap();
        for (String key : tag.getAllKeys()) {
            list.put(key, PerWorldConfig.get().toObject(tag.get(key), Vec3i.class));
        }
        return list;
    }

    public CompoundTag parse(String s) {
        return new CompoundTag();
    }
}
