package at.haha007.edenclient.mods.chestshop;

import at.haha007.edenclient.utils.config.loaders.ConfigLoader;
import net.minecraft.nbt.CompoundTag;

public class ChestShopEntryLoader implements ConfigLoader<CompoundTag, ChestShopEntry> {

    public CompoundTag save(Object value) {
        return cast(value).toTag();
    }

    public ChestShopEntry load(CompoundTag tag) {
        return new ChestShopEntry(tag);
    }

    public CompoundTag parse(String s) {
        return null;
    }
}
