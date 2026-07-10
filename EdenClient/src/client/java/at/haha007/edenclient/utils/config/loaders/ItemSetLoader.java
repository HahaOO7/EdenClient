package at.haha007.edenclient.utils.config.loaders;

import at.haha007.edenclient.utils.config.PerWorldConfig;
import at.haha007.edenclient.utils.config.wrappers.ItemSet;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.NotNull;

public class ItemSetLoader implements ConfigLoader<ListTag, ItemSet> {

    @NotNull
    public ListTag save(@NotNull ItemSet list) {
        ListTag nbt = new ListTag();
        for (Item item : list) {
            nbt.add(PerWorldConfig.get().toNbt(item));
        }
        return nbt;
    }

    @NotNull
    public ItemSet load(@NotNull ListTag nbtElement) {
        ItemSet list = new ItemSet();
        for (Tag nbt : nbtElement) {
            list.add(PerWorldConfig.get().toObject(nbt, Item.class));
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