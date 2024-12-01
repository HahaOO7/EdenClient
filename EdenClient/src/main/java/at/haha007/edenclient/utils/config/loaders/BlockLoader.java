package at.haha007.edenclient.utils.config.loaders;

import net.minecraft.core.DefaultedRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.StringTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.NotNull;

public class BlockLoader implements ConfigLoader<StringTag, Block> {

    private static final DefaultedRegistry<Block> registry = BuiltInRegistries.BLOCK;

    @NotNull
    public StringTag save(@NotNull Block value) {
        return StringTag.valueOf(registry.getKey(value).toString());
    }

    @NotNull
    public Block load(@NotNull StringTag nbtElement) {
        return registry.getValue(ResourceLocation.parse(nbtElement.getAsString()));
    }

    @NotNull
    public StringTag parse(@NotNull String s) {
        return StringTag.valueOf("minecraft:" + s);
    }

}