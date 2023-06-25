package at.haha007.edenclient.utils.config.loaders;

import java.util.Arrays;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

public class BlockBoxLoader implements ConfigLoader<IntArrayTag, BoundingBox> {

    public IntArrayTag save(Object value) {
        BoundingBox bb = cast(value);
        return new IntArrayTag(new int[]{
                bb.minX(),
                bb.maxX(),
                bb.minY(),
                bb.maxY(),
                bb.minZ(),
                bb.maxZ()
        });
    }

    public BoundingBox load(IntArrayTag tag) {
        int[] ints = tag.stream().mapToInt(IntTag::getAsInt).toArray();
        return new BoundingBox(ints[0], ints[2], ints[4], ints[1], ints[3], ints[5]);
    }

    public IntArrayTag parse(String s) {
        if (s.isEmpty()) return new IntArrayTag(new int[]{0, 0, 0, 0, 0, 0});
        int[] ints = Arrays.stream(s.split(",")).mapToInt(Integer::parseInt).toArray();
        return new IntArrayTag(ints);
    }
}
