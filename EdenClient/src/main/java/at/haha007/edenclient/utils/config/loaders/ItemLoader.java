package at.haha007.edenclient.utils.config.loaders;

import net.minecraft.core.DefaultedRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.StringTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.NotNull;

public class ItemLoader implements ConfigLoader<StringTag, Item> {

    private static final DefaultedRegistry<Item> registry = BuiltInRegistries.ITEM;

    @NotNull
    public StringTag save(@NotNull Item value) {
        return StringTag.valueOf(registry.getKey(value).toString());
    }

    @NotNull
    public Item load(@NotNull StringTag nbtElement) {
        return registry.getValue(ResourceLocation.parse(nbtElement.getAsString()));
    }

    @NotNull
    public StringTag parse(@NotNull String s) {
        return StringTag.valueOf("minecraft:" + s);
    }
}
