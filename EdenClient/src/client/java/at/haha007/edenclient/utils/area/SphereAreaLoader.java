package at.haha007.edenclient.utils.area;

import at.haha007.edenclient.utils.config.loaders.ConfigLoader;
import at.haha007.edenclient.utils.config.loaders.Vec3iLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class SphereAreaLoader implements ConfigLoader<CompoundTag, SphereArea> {

    @Override
    @NotNull
    public CompoundTag save(@NotNull SphereArea value) {
        CompoundTag tag = new CompoundTag();
        Vec3iLoader loader = new Vec3iLoader();
        IntArrayTag centerTag = loader.save(value.getCenter());
        tag.put("center", centerTag);
        tag.putDouble("radius", value.getRadius());
        return tag;
    }

    @Override
    @NotNull
    public SphereArea load(@NotNull CompoundTag nbtElement) {
        Vec3iLoader loader = new Vec3iLoader();
        Vec3i center = loader.load((IntArrayTag) Objects.requireNonNull(nbtElement.get("center")));
        double radius = nbtElement.getDouble("radius").orElseThrow();
        return new SphereArea(new BlockPos(center), radius);
    }

    @Override
    @NotNull
    public CompoundTag parse(@NotNull String s) {
        CompoundTag tag = new CompoundTag();
        Vec3iLoader vec3iLoader = new Vec3iLoader();
        tag.put("center", vec3iLoader.save(Vec3i.ZERO));
        tag.putDouble("radius", -1);
        return tag;
    }

    public ConfigLoader<CompoundTag, BlockArea> asBlockAreaLoader() {
        return new ConfigLoader<>() {

            @NotNull
            @Override
            public CompoundTag save(@NotNull BlockArea value) {
                if (!(value instanceof SphereArea sphereArea))
                    throw new IllegalArgumentException("Invalid block area type");
                return SphereAreaLoader.this.save(sphereArea);
            }

            @NotNull
            @Override
            public BlockArea load(@NotNull CompoundTag nbtElement) {
                return SphereAreaLoader.this.load(nbtElement);
            }

            @NotNull
            @Override
            public CompoundTag parse(@NotNull String s) {
                return SphereAreaLoader.this.parse(s);
            }
        };
    }
}
