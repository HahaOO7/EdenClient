package at.haha007.edenclient.utils.config.loaders;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.StringTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class BlockEntityTypeLoader implements ConfigLoader<StringTag, BlockEntityType<?>> {

    private static final Registry<BlockEntityType<?>> registry = BuiltInRegistries.BLOCK_ENTITY_TYPE;

    public StringTag save(BlockEntityType<?> value) {
        return StringTag.valueOf(String.valueOf(registry.getKey(value)));
    }

    public BlockEntityType<?> load(StringTag nbtElement) {
        return registry.get(new ResourceLocation(nbtElement.getAsString()));
    }

    public StringTag parse(String s) {
        return StringTag.valueOf("minecraft:" + s);
    }
}
