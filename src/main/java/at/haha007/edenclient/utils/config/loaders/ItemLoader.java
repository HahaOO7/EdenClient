package at.haha007.edenclient.utils.config.loaders;

import net.minecraft.item.Item;
import net.minecraft.nbt.NbtString;
import net.minecraft.registry.DefaultedRegistry;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

public class ItemLoader implements ConfigLoader<NbtString, Item> {

    private static final DefaultedRegistry<Item> registry = Registries.ITEM;

    public NbtString save(Object value) {
        return NbtString.of(registry.getId(cast(value)).toString());
    }

    public Item load(NbtString nbtElement) {
        return registry.get(new Identifier(nbtElement.asString()));
    }

    public NbtString parse(String s) {
        return NbtString.of("minecraft:" + s);
    }
}
