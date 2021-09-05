package at.haha007.edenclient.utils.config.loaders;

import at.haha007.edenclient.utils.config.PerWorldConfig;
import at.haha007.edenclient.utils.config.wrappers.BlockSet;
import net.minecraft.block.Block;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;

public class BlockSetLoader implements ConfigLoader<NbtList, BlockSet> {

    public NbtList save(Object value) {
        BlockSet list = cast(value);
        NbtList nbt = new NbtList();
        for (Block item : list) {
            nbt.add(PerWorldConfig.get().toNbt(item));
        }
        return nbt;
    }

    public BlockSet load(NbtList nbtElement) {
        BlockSet list = new BlockSet();
        for (NbtElement nbt : nbtElement) {
            list.add(PerWorldConfig.get().toObject(nbt, Block.class));
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
