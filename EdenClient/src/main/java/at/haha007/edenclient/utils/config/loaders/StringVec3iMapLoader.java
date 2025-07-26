package at.haha007.edenclient.utils.config.loaders;

import at.haha007.edenclient.utils.config.PerWorldConfig;
import at.haha007.edenclient.utils.config.wrappers.StringVec3iMap;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.NotNull;

public class StringVec3iMapLoader implements ConfigLoader<CompoundTag, StringVec3iMap> {

    @NotNull
    public CompoundTag save(@NotNull StringVec3iMap value) {
        CompoundTag tag = new CompoundTag();
        value.forEach((k, v) -> tag.put(k, PerWorldConfig.get().toNbt(v)));
        return tag;
    }

    @NotNull
    public StringVec3iMap load(@NotNull CompoundTag tag) {
        StringVec3iMap list = new StringVec3iMap();
        for (String key : tag.keySet()) {
            list.put(key, PerWorldConfig.get().toObject(tag.get(key), Vec3i.class));
        }
        return list;
    }

    @NotNull
    public CompoundTag parse(@NotNull String s) {
        return new CompoundTag();
    }
}
