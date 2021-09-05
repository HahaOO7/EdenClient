package at.haha007.edenclient.utils.config.loaders;

import net.minecraft.block.Block;
import net.minecraft.nbt.NbtString;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.DefaultedRegistry;
import net.minecraft.util.registry.Registry;

public class BlockLoader implements ConfigLoader<NbtString, Block> {

    private static final DefaultedRegistry<Block> registry = Registry.BLOCK;

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