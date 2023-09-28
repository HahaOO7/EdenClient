package at.haha007.edenclient.utils.config.loaders;

import net.minecraft.core.Vec3i;
import net.minecraft.nbt.IntArrayTag;

import static java.lang.Integer.parseInt;

public class Vec3iLoader implements ConfigLoader<IntArrayTag, Vec3i> {

    public IntArrayTag save(Object value) {
        Vec3i v = cast(value);
        return new IntArrayTag(new int[]{v.getX(), v.getY(), v.getZ()});
    }

    public Vec3i load(IntArrayTag tag) {
        return new Vec3i(tag.get(0).getAsInt(), tag.get(1).getAsInt(), tag.get(2).getAsInt());
    }

    public IntArrayTag parse(String s) {
        if (s.isEmpty()) return new IntArrayTag(new int[]{0, 0, 0});
        String[] a = s.split(",");
        return new IntArrayTag(new int[]{parseInt(a[0]), parseInt(a[1]), parseInt(a[2])});
    }
}
