package at.haha007.edenclient.utils.config.loaders;

import at.haha007.edenclient.utils.config.PerWorldConfig;
import at.haha007.edenclient.utils.config.wrappers.ItemList;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.world.item.Item;

public class ItemListLoader implements ConfigLoader<ListTag, ItemList> {

    public ListTag save(ItemList list) {
        ListTag tag = new ListTag();
        list.forEach(item -> tag.add(PerWorldConfig.get().toNbt(item)));
        return tag;
    }

    public ItemList load(ListTag tag) {
        ItemList list = new ItemList();
        tag.forEach(entry -> list.add(PerWorldConfig.get().toObject(entry, Item.class)));
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
