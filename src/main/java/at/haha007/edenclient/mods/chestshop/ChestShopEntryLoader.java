package at.haha007.edenclient.mods.chestshop;

import at.haha007.edenclient.utils.config.NbtLoadable;
import at.haha007.edenclient.utils.config.loaders.ConfigLoader;
import net.minecraft.nbt.NbtCompound;

public class ChestShopEntryLoader implements ConfigLoader<NbtCompound, ChestShopEntry> {

    public NbtCompound save(Object value) {
        return cast(value).toTag();
    }

    public ChestShopEntry load(NbtCompound tag) {
        return new ChestShopEntry(tag);
    }

    public NbtCompound parse(String s) {
        return null;
    }
}
