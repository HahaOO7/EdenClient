package at.haha007.edenclient.utils.config.loaders;

import net.minecraft.nbt.IntArrayTag;
import net.minecraft.world.level.ChunkPos;

public class ChunkPosLoader implements ConfigLoader<IntArrayTag, ChunkPos> {
    public IntArrayTag save(Object value) {
        ChunkPos cp = cast(value);
        return new IntArrayTag(new int[]{cp.x, cp.z});
    }

    public ChunkPos load(IntArrayTag tag) {
        return new ChunkPos(tag.get(0).getAsInt(), tag.get(1).getAsInt());
    }

    public IntArrayTag parse(String s) {
        return new IntArrayTag(new int[]{0, 0});
    }
}
