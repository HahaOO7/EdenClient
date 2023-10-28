package at.haha007.edenclient.utils.config.loaders;

import at.haha007.edenclient.utils.config.PerWorldConfig;
import at.haha007.edenclient.utils.config.wrappers.EntityTypeSet;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.EntityType;

public class EntityTypeSetLoader implements ConfigLoader<ListTag, EntityTypeSet> {

    public ListTag save(EntityTypeSet list) {
        ListTag nbt = new ListTag();
        for (EntityType<?> item : list) {
            nbt.add(PerWorldConfig.get().toNbt(item));
        }
        return nbt;
    }

    public EntityTypeSet load(ListTag nbtElement) {
        EntityTypeSet list = new EntityTypeSet();
        for (Tag nbt : nbtElement) {
            list.add(PerWorldConfig.get().toObject(nbt, EntityType.class));
        }
        return list;
    }

    public ListTag parse(String s) {
        ListTag list = new ListTag();
        if (s.isEmpty()) return list;
        for (String it : s.split(",")) {
            list.add(StringTag.valueOf("minecraft:" + it));
        }
        return list;
    }
}
