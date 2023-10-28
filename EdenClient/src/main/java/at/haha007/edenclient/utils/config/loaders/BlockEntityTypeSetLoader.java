package at.haha007.edenclient.utils.config.loaders;

import at.haha007.edenclient.utils.config.PerWorldConfig;
import at.haha007.edenclient.utils.config.wrappers.BlockEntityTypeSet;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class BlockEntityTypeSetLoader implements ConfigLoader<ListTag, BlockEntityTypeSet> {

    public ListTag save(BlockEntityTypeSet list) {
        ListTag nbt = new ListTag();
        for (BlockEntityType<?> item : list) {
            nbt.add(PerWorldConfig.get().toNbt(item));
        }
        return nbt;
    }

    public BlockEntityTypeSet load(ListTag nbtElement) {
        BlockEntityTypeSet list = new BlockEntityTypeSet();
        for (Tag nbt : nbtElement) {
            list.add(PerWorldConfig.get().toObject(nbt, BlockEntityType.class));
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
