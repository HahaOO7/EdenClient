package at.haha007.edenclient.utils.config.loaders;

import at.haha007.edenclient.utils.config.PerWorldConfig;
import at.haha007.edenclient.utils.config.wrappers.BlockEntityTypeSet;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.jetbrains.annotations.NotNull;

public class BlockEntityTypeSetLoader implements ConfigLoader<ListTag, BlockEntityTypeSet> {

    @NotNull
    public ListTag save(@NotNull BlockEntityTypeSet list) {
        ListTag nbt = new ListTag();
        for (BlockEntityType<?> item : list) {
            nbt.add(PerWorldConfig.get().toNbt(item));
        }
        return nbt;
    }

    @NotNull
    public BlockEntityTypeSet load(@NotNull ListTag nbtElement) {
        BlockEntityTypeSet list = new BlockEntityTypeSet();
        for (Tag nbt : nbtElement) {
            list.add(PerWorldConfig.get().toObject(nbt, BlockEntityType.class));
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
