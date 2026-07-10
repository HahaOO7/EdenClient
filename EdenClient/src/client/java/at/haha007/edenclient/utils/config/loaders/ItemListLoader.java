package at.haha007.edenclient.utils.config.loaders;

import at.haha007.edenclient.utils.config.PerWorldConfig;
import at.haha007.edenclient.utils.config.wrappers.ItemList;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.NotNull;

public class ItemListLoader implements ConfigLoader<ListTag, ItemList> {

    @NotNull
    public ListTag save(@NotNull ItemList list) {
        ListTag tag = new ListTag();
        list.forEach(item -> tag.add(PerWorldConfig.get().toNbt(item)));
        return tag;
    }

    @NotNull
    public ItemList load(@NotNull ListTag tag) {
        ItemList list = new ItemList();
        tag.forEach(entry -> list.add(PerWorldConfig.get().toObject(entry, Item.class)));
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
