package at.haha007.edenclient.mods.chestshop;

import at.haha007.edenclient.utils.config.loaders.ConfigLoader;
import java.util.Collection;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

public class ChestShopLoader implements ConfigLoader<ListTag, ChestShopMap> {

    public ListTag save(ChestShopMap map) {
        ListTag list = new ListTag();
        map.values().stream()
                .flatMap(Collection::stream)
                .map(ChestShopEntry::toTag)
                .forEach(list::add);
        return list;
    }

    public ChestShopMap load(ListTag tag) {
        ChestShopMap map = new ChestShopMap();
        for (Tag element : tag) {
            ChestShopEntry entry = new ChestShopEntry((CompoundTag) element);
            ChestShopSet set = map.computeIfAbsent(entry.getChunkPos(), k -> new ChestShopSet());
            set.add(entry);
        }
        return map;
    }

    public ListTag parse(String s) {
        return new ListTag();
    }
}
