package at.haha007.edenclient.utils.config.loaders;

import net.minecraft.nbt.NbtInt;
import net.minecraft.nbt.NbtIntArray;
import net.minecraft.util.math.BlockBox;

import java.util.Arrays;

public class BlockBoxLoader implements ConfigLoader<NbtIntArray, BlockBox> {

    public NbtIntArray save(Object value) {
        BlockBox bb = cast(value);
        return new NbtIntArray(new int[]{
                bb.getMinX(),
                bb.getMaxX(),
                bb.getMinY(),
                bb.getMaxY(),
                bb.getMinZ(),
                bb.getMaxZ()
        });
    }

    public BlockBox load(NbtIntArray tag) {
        int[] ints = tag.stream().mapToInt(NbtInt::intValue).toArray();
        return new BlockBox(ints[0], ints[2], ints[4], ints[1], ints[3], ints[5]);
    }

    public NbtIntArray parse(String s) {
        if (s.isEmpty()) return new NbtIntArray(new int[]{0, 0, 0, 0, 0, 0});
        int[] ints = Arrays.stream(s.split(",")).mapToInt(Integer::parseInt).toArray();
        return new NbtIntArray(ints);
    }
}
