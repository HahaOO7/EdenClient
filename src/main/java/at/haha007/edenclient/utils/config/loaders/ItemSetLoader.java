package at.haha007.edenclient.utils.config.loaders;

import at.haha007.edenclient.utils.config.PerWorldConfig;
import at.haha007.edenclient.utils.config.wrappers.ItemSet;
import net.minecraft.item.Item;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;

import java.util.Set;

public class ItemSetLoader implements ConfigLoader<NbtList, ItemSet> {

    public NbtList save(Object value) {
        Set<Item> list = cast(value);
        NbtList nbt = new NbtList();
        for (Item item : list) {
            nbt.add(PerWorldConfig.get().toNbt(item));
        }
        return nbt;
    }

    public ItemSet load(NbtList nbtElement) {
        ItemSet list = new ItemSet();
        for (NbtElement nbt : nbtElement) {
            list.add(PerWorldConfig.get().toObject(nbt, Item.class));
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