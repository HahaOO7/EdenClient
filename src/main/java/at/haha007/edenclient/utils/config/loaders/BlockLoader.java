package at.haha007.edenclient.utils.config.loaders;

import net.minecraft.block.Block;
import net.minecraft.nbt.NbtString;
import net.minecraft.registry.DefaultedRegistry;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

public class BlockLoader implements ConfigLoader<NbtString, Block> {

    private static final DefaultedRegistry<Block> registry = Registries.BLOCK;

    public NbtString save(Object value) {
        return NbtString.of(registry.getId(cast(value)).toString());
    }

    public Block load(NbtString nbtElement) {
        return registry.get(new Identifier(nbtElement.asString()));
    }

    public NbtString parse(String s) {
        return NbtString.of("minecraft:" + s);
    }

}