package at.haha007.edenclient.utils.config.loaders;

import net.minecraft.core.DefaultedRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.StringTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

public class ItemLoader implements ConfigLoader<StringTag, Item> {

    private static final DefaultedRegistry<Item> registry = BuiltInRegistries.ITEM;

    public StringTag save(Object value) {
        return StringTag.valueOf(registry.getKey(cast(value)).toString());
    }

    public Item load(StringTag nbtElement) {
        return registry.get(new ResourceLocation(nbtElement.getAsString()));
    }

    public StringTag parse(String s) {
        return StringTag.valueOf("minecraft:" + s);
    }
}
