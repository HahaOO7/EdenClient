package at.haha007.edenclient.utils.config.loaders;

import at.haha007.edenclient.utils.config.PerWorldConfig;
import at.haha007.edenclient.utils.config.wrappers.BlockSet;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.Block;

public class BlockSetLoader implements ConfigLoader<ListTag, BlockSet> {

    public ListTag save(BlockSet list) {
        ListTag nbt = new ListTag();
        for (Block item : list) {
            nbt.add(PerWorldConfig.get().toNbt(item));
        }
        return nbt;
    }

    public BlockSet load(ListTag nbtElement) {
        BlockSet list = new BlockSet();
        for (Tag nbt : nbtElement) {
            list.add(PerWorldConfig.get().toObject(nbt, Block.class));
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
