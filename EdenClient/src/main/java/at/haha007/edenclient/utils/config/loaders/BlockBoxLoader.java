package at.haha007.edenclient.utils.config.loaders;

import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

public class BlockBoxLoader implements ConfigLoader<IntArrayTag, BoundingBox> {

    @NotNull
    public IntArrayTag save(@NotNull BoundingBox bb) {
        return new IntArrayTag(new int[]{
                bb.minX(),
                bb.maxX(),
                bb.minY(),
                bb.maxY(),
                bb.minZ(),
                bb.maxZ()
        });
    }

    @NotNull
    public BoundingBox load(@NotNull IntArrayTag tag) {
        int[] ints = tag.stream().mapToInt(IntTag::getAsInt).toArray();
        return new BoundingBox(ints[0], ints[2], ints[4], ints[1], ints[3], ints[5]);
    }

    @NotNull
    public IntArrayTag parse(@NotNull String s) {
        if (s.isEmpty()) return new IntArrayTag(new int[]{0, 0, 0, 0, 0, 0});
        int[] ints = Arrays.stream(s.split(",")).mapToInt(Integer::parseInt).toArray();
        return new IntArrayTag(ints);
    }
}
