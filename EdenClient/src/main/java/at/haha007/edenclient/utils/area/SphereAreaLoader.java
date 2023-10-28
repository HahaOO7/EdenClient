package at.haha007.edenclient.utils.area;

import at.haha007.edenclient.utils.config.loaders.ConfigLoader;
import at.haha007.edenclient.utils.config.loaders.Vec3iLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;

import java.util.Objects;

public class SphereAreaLoader implements ConfigLoader<CompoundTag, SphereArea> {

    @Override
    public CompoundTag save(SphereArea value) {
        CompoundTag tag = new CompoundTag();
        Vec3iLoader loader = new Vec3iLoader();
        IntArrayTag centerTag = loader.save(value.getCenter());
        tag.put("center", centerTag);
        tag.putDouble("radius", value.getRadius());
        return tag;
    }

    @Override
    public SphereArea load(CompoundTag nbtElement) {
        Vec3iLoader loader = new Vec3iLoader();
        Vec3i center = loader.load((IntArrayTag) Objects.requireNonNull(nbtElement.get("center")));
        double radius = nbtElement.getDouble("radius");
        return new SphereArea(new BlockPos(center), radius);
    }

    @Override
    public CompoundTag parse(String s) {
        return null;
    }

    public ConfigLoader<CompoundTag, BlockArea> asBlockAreaLoader() {
        return new ConfigLoader<CompoundTag, BlockArea>() {
            @Override
            public CompoundTag save(BlockArea value) {
                if (!(value instanceof SphereArea sphereArea))
                    throw new IllegalArgumentException("Invalid block area type");
                return SphereAreaLoader.this.save(sphereArea);
            }

            @Override
            public BlockArea load(CompoundTag nbtElement) {
                return SphereAreaLoader.this.load(nbtElement);
            }

            @Override
            public CompoundTag parse(String s) {
                return SphereAreaLoader.this.parse(s);
            }
        };
    }
}
