package at.haha007.edenclient.utils.config.loaders;

import at.haha007.edenclient.utils.config.PerWorldConfig;
import at.haha007.edenclient.utils.config.wrappers.BlockSet;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.NotNull;

public class BlockSetLoader implements ConfigLoader<ListTag, BlockSet> {

    @NotNull
    public ListTag save(@NotNull BlockSet list) {
        ListTag nbt = new ListTag();
        for (Block item : list) {
            nbt.add(PerWorldConfig.get().toNbt(item));
        }
        return nbt;
    }

    @NotNull
    public BlockSet load(@NotNull ListTag nbtElement) {
        BlockSet list = new BlockSet();
        for (Tag nbt : nbtElement) {
            list.add(PerWorldConfig.get().toObject(nbt, Block.class));
        }
        return list;
    }

    @NotNull
    public ListTag parse(@NotNull String s) {
        ListTag list = new ListTag();
        if (s.isEmpty()) return list;
        for (String it : s.split(",")) {
            list.add(StringTag.valueOf("minecraft:" + it));
        }
        return list;
    }
}
