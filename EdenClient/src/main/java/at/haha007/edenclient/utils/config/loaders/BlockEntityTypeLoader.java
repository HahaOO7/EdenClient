package at.haha007.edenclient.utils.config.loaders;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.StringTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.jetbrains.annotations.NotNull;

public class BlockEntityTypeLoader implements ConfigLoader<StringTag, BlockEntityType<?>> {

    private static final Registry<BlockEntityType<?>> registry = BuiltInRegistries.BLOCK_ENTITY_TYPE;

    @NotNull
    public StringTag save(@NotNull BlockEntityType<?> value) {
        return StringTag.valueOf(String.valueOf(registry.getKey(value)));
    }

    @NotNull
    public BlockEntityType<?> load(@NotNull StringTag nbtElement) {
        BlockEntityType<?> type = registry.getValue(ResourceLocation.parse(nbtElement.getAsString()));
        return type == null ? BlockEntityType.STRUCTURE_BLOCK : type;
    }

    @NotNull
    public StringTag parse(@NotNull String s) {
        return StringTag.valueOf("minecraft:" + s);
    }
}
