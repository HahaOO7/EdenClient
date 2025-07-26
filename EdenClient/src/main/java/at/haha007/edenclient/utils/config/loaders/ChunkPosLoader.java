package at.haha007.edenclient.utils.config.loaders;

import net.minecraft.nbt.IntArrayTag;
import net.minecraft.world.level.ChunkPos;
import org.jetbrains.annotations.NotNull;

public class ChunkPosLoader implements ConfigLoader<IntArrayTag, ChunkPos> {
    @NotNull
    public IntArrayTag save(@NotNull ChunkPos cp) {
        return new IntArrayTag(new int[]{cp.x, cp.z});
    }

    @NotNull
    public ChunkPos load(@NotNull IntArrayTag tag) {
        return new ChunkPos(tag.get(0).asInt().orElseThrow(), tag.get(1).asInt().orElseThrow());
    }

    @NotNull
    public IntArrayTag parse(@NotNull String s) {
        return new IntArrayTag(new int[]{0, 0});
    }
}
