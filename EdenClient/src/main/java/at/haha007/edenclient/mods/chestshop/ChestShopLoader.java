package at.haha007.edenclient.mods.chestshop;

import at.haha007.edenclient.utils.config.loaders.ConfigLoader;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public class ChestShopLoader implements ConfigLoader<ListTag, ChestShopMap> {

    @NotNull
    public ListTag save(@NotNull ChestShopMap map) {
        ListTag list = new ListTag();
        map.values().stream()
                .flatMap(Collection::stream)
                .map(ChestShopEntry::toTag)
                .forEach(list::add);
        return list;
    }

    @NotNull
    public ChestShopMap load(@NotNull ListTag tag) {
        ChestShopMap map = new ChestShopMap();
        for (Tag element : tag) {
            ChestShopEntry entry = new ChestShopEntry((CompoundTag) element);
            ChestShopSet set = map.computeIfAbsent(entry.getChunkPos(), k -> new ChestShopSet());
            set.add(entry);
        }
        return map;
    }

    @NotNull
    public ListTag parse(@NotNull String s) {
        return new ListTag();
    }
}
