package at.haha007.edenclient.utils.area;

import at.haha007.edenclient.utils.config.loaders.ConfigLoader;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

public class CubeAreaLoader implements ConfigLoader<CompoundTag, CubeArea> {

    @NotNull
    public CompoundTag save(@NotNull CubeArea area) {
        int minX = area.getBox().minX();
        int minY = area.getBox().minY();
        int minZ = area.getBox().minZ();
        int maxX = area.getBox().maxX();
        int maxY = area.getBox().maxY();
        int maxZ = area.getBox().maxZ();
        CompoundTag tag = new CompoundTag();
        tag.putInt("minX", minX);
        tag.putInt("minY", minY);
        tag.putInt("minZ", minZ);
        tag.putInt("maxX", maxX);
        tag.putInt("maxY", maxY);
        tag.putInt("maxZ", maxZ);
        return tag;
    }

    @Override
    @NotNull
    public CubeArea load(@NotNull CompoundTag nbtElement) {
        int minX = nbtElement.getInt("minX");
        int minY = nbtElement.getInt("minY");
        int minZ = nbtElement.getInt("minZ");
        int maxX = nbtElement.getInt("maxX");
        int maxY = nbtElement.getInt("maxY");
        int maxZ = nbtElement.getInt("maxZ");
        return new CubeArea(new Vec3i(minX, minY, minZ), new Vec3i(maxX, maxY, maxZ));
    }

    @Override
    @NotNull
    public CompoundTag parse(@NotNull String s) {
        if (s.isEmpty()) return new CompoundTag();
        String[] a = s.split(",");
        int[] ints = Arrays.stream(a).mapToInt(Integer::parseInt).toArray();
        CompoundTag tag = new CompoundTag();
        tag.putInt("minX", ints[0]);
        tag.putInt("minY", ints[1]);
        tag.putInt("minZ", ints[2]);
        tag.putInt("maxX", ints[3]);
        tag.putInt("maxY", ints[4]);
        tag.putInt("maxZ", ints[5]);
        return tag;
    }

    public ConfigLoader<CompoundTag, BlockArea> asBlockAreaLoader() {
        return new ConfigLoader<>() {
            @Override
            @NotNull
            public CompoundTag save(@NotNull BlockArea value) {
                if (!(value instanceof CubeArea cubeArea))
                    throw new IllegalArgumentException("Invalid block area type");
                return CubeAreaLoader.this.save(cubeArea);
            }

            @Override
            @NotNull
            public BlockArea load(@NotNull CompoundTag nbtElement) {
                return CubeAreaLoader.this.load(nbtElement);
            }

            @Override
            @NotNull
            public CompoundTag parse(@NotNull String s) {
                return CubeAreaLoader.this.parse(s);
            }
        };
    }
}
