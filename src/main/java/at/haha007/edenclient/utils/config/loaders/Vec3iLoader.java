package at.haha007.edenclient.utils.config.loaders;

import net.minecraft.nbt.NbtIntArray;
import net.minecraft.util.math.Vec3i;

import static java.lang.Integer.parseInt;

public class Vec3iLoader implements ConfigLoader<NbtIntArray, Vec3i> {

    public NbtIntArray save(Object value) {
        Vec3i v = cast(value);
        return new NbtIntArray(new int[]{v.getX(), v.getY(), v.getZ()});
    }

    public Vec3i load(NbtIntArray tag) {
        return new Vec3i(tag.get(0).intValue(), tag.get(1).intValue(), tag.get(2).intValue());
    }

    public NbtIntArray parse(String s) {
        if (s.isEmpty()) return new NbtIntArray(new int[]{0, 0, 0});
        String[] a = s.split(",");
        return new NbtIntArray(new int[]{parseInt(a[0]), parseInt(a[1]), parseInt(a[2])});
    }
}
