package at.haha007.edenclient.mods.chestshop;

import at.haha007.edenclient.utils.config.loaders.ConfigLoader;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;

import java.util.Collection;

public class ChestShopLoader implements ConfigLoader<NbtList, ChestShopMap> {

    public NbtList save(Object value) {
        ChestShopMap map = cast(value);
        NbtList list = new NbtList();
        map.values().stream()
                .flatMap(Collection::stream)
                .map(ChestShopEntry::toTag)
                .forEach(list::add);
        return list;
    }

    public ChestShopMap load(NbtList tag) {
        ChestShopMap map = new ChestShopMap();
        for (NbtElement element : tag) {
            ChestShopEntry entry = new ChestShopEntry((NbtCompound) element);
            ChestShopSet set = map.computeIfAbsent(entry.getChunkPos(), k -> new ChestShopSet());
            set.add(entry);
        }
        return map;
    }

    public NbtList parse(String s) {
        return new NbtList();
    }
}
