package at.haha007.edenclient.utils.config.loaders;

import at.haha007.edenclient.utils.config.PerWorldConfig;
import at.haha007.edenclient.utils.config.wrappers.BlockEntityTypeSet;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;

public class BlockEntityTypeSetLoader implements ConfigLoader<NbtList, BlockEntityTypeSet> {

    public NbtList save(Object value) {
        BlockEntityTypeSet list = cast(value);
        NbtList nbt = new NbtList();
        for (BlockEntityType<?> item : list) {
            nbt.add(PerWorldConfig.get().toNbt(item));
        }
        return nbt;
    }

    public BlockEntityTypeSet load(NbtList nbtElement) {
        BlockEntityTypeSet list = new BlockEntityTypeSet();
        for (NbtElement nbt : nbtElement) {
            list.add(PerWorldConfig.get().toObject(nbt, BlockEntityType.class));
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
