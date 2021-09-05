package at.haha007.edenclient.utils.config.loaders;

import at.haha007.edenclient.utils.config.PerWorldConfig;
import at.haha007.edenclient.utils.config.wrappers.EntityTypeSet;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;

public class EntityTypeSetLoader implements ConfigLoader<NbtList, EntityTypeSet> {

    public NbtList save(Object value) {
        EntityTypeSet list = cast(value);
        NbtList nbt = new NbtList();
        for (EntityType<?> item : list) {
            nbt.add(PerWorldConfig.get().toNbt(item));
        }
        return nbt;
    }

    public EntityTypeSet load(NbtList nbtElement) {
        EntityTypeSet list = new EntityTypeSet();
        for (NbtElement nbt : nbtElement) {
            list.add(PerWorldConfig.get().toObject(nbt, EntityType.class));
        }
        return list;
    }

    public NbtList parse(String s) {
        NbtList list = new NbtList();
        if (s.isEmpty()) return list;
        for (String it : s.split(",")) {
            list.add(NbtString.of("minecraft:" + it));
        }
        return list;
    }
}
