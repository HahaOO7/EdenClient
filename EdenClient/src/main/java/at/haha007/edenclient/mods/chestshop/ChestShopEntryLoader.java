package at.haha007.edenclient.mods.chestshop;

import at.haha007.edenclient.utils.config.loaders.ConfigLoader;
import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.NotNull;

public class ChestShopEntryLoader implements ConfigLoader<CompoundTag, ChestShopEntry> {

    @NotNull
    public CompoundTag save(@NotNull ChestShopEntry value) {
        return value.toTag();
    }

    @NotNull
    public ChestShopEntry load(@NotNull CompoundTag tag) {
        return new ChestShopEntry(tag);
    }

    @NotNull
    public CompoundTag parse(@NotNull String s) {
        CompoundTag tag = new CompoundTag();
        tag.putIntArray("pos", new int[]{0, 0, 0});
        tag.putString("owner", "owner");
        tag.putString("item", "stone");
        tag.putInt("amount", 0);
        tag.putInt("stock", 0);
        return tag;
    }
}
