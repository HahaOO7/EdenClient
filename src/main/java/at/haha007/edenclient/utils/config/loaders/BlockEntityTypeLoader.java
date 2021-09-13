package at.haha007.edenclient.utils.config.loaders;

import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.nbt.NbtString;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

public class BlockEntityTypeLoader implements ConfigLoader<NbtString, BlockEntityType<?>> {

    private static final Registry<BlockEntityType<?>> registry = Registry.BLOCK_ENTITY_TYPE;

    public NbtString save(Object value) {
        return NbtString.of(registry.getId(cast(value)).toString());
    }

    public BlockEntityType<?> load(NbtString nbtElement) {
        return registry.get(new Identifier(nbtElement.asString()));
    }

    public NbtString parse(String s) {
        return NbtString.of("minecraft:" + s);
    }
}
