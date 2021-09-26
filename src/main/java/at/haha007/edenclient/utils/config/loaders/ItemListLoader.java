package at.haha007.edenclient.utils.config.loaders;

import at.haha007.edenclient.utils.config.PerWorldConfig;
import at.haha007.edenclient.utils.config.wrappers.ItemList;
import net.minecraft.item.Item;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;

public class ItemListLoader implements ConfigLoader<NbtList, ItemList> {

    public NbtList save(Object value) {
        ItemList list = cast(value);
        NbtList tag = new NbtList();

        list.forEach(item -> tag.add(PerWorldConfig.get().toNbt(item)));
        return tag;
    }

    public ItemList load(NbtList tag) {
        ItemList list = new ItemList();
        tag.forEach(entry -> list.add(PerWorldConfig.get().toObject(entry, Item.class)));
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
