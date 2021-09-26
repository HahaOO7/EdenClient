package at.haha007.edenclient.utils.config.loaders;

import net.minecraft.nbt.NbtIntArray;
import net.minecraft.util.math.ChunkPos;

public class ChunkPosLoader implements ConfigLoader<NbtIntArray, ChunkPos> {
    public NbtIntArray save(Object value) {
        ChunkPos cp = cast(value);
        return new NbtIntArray(new int[]{cp.x, cp.z});
    }

    public ChunkPos load(NbtIntArray tag) {
        return new ChunkPos(tag.get(0).intValue(), tag.get(1).intValue());
    }

    public NbtIntArray parse(String s) {
        return new NbtIntArray(new int[]{0, 0});
    }
}
