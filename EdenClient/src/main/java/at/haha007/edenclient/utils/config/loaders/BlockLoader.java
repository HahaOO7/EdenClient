package at.haha007.edenclient.utils.config.loaders;

import net.minecraft.core.DefaultedRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.StringTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;

public class BlockLoader implements ConfigLoader<StringTag, Block> {

    private static final DefaultedRegistry<Block> registry = BuiltInRegistries.BLOCK;

    public StringTag save(Block value) {
        return StringTag.valueOf(registry.getKey(value).toString());
    }

    public Block load(StringTag nbtElement) {
        return registry.get(new ResourceLocation(nbtElement.getAsString()));
    }

    public StringTag parse(String s) {
        return StringTag.valueOf("minecraft:" + s);
    }

}