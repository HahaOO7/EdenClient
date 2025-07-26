package at.haha007.edenclient.utils.config.loaders;

import net.minecraft.core.Vec3i;
import net.minecraft.nbt.IntArrayTag;
import org.jetbrains.annotations.NotNull;

import static java.lang.Integer.parseInt;

public class Vec3iLoader implements ConfigLoader<IntArrayTag, Vec3i> {

    @NotNull
    public IntArrayTag save(@NotNull Vec3i v) {
        return new IntArrayTag(new int[]{v.getX(), v.getY(), v.getZ()});
    }

    @NotNull
    public Vec3i load(@NotNull IntArrayTag tag) {
        return new Vec3i(tag.get(0).asInt().orElseThrow(), tag.get(1).asInt().orElseThrow(), tag.get(2).asInt().orElseThrow());
    }

    @NotNull
    public  IntArrayTag parse(@NotNull String s) {
        if (s.isEmpty()) return new IntArrayTag(new int[]{0, 0, 0});
        String[] a = s.split(",");
        return new IntArrayTag(new int[]{parseInt(a[0]), parseInt(a[1]), parseInt(a[2])});
    }
}
