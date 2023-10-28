package at.haha007.edenclient.utils.area;

import at.haha007.edenclient.utils.config.loaders.ConfigLoader;
import at.haha007.edenclient.utils.config.loaders.Vec3iLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;

import java.util.Objects;

public class CylinderAreaLoader implements ConfigLoader<CompoundTag, CylinderArea> {

    public CompoundTag save(CylinderArea value) {
        CompoundTag tag = new CompoundTag();
        Vec3iLoader loader = new Vec3iLoader();
        IntArrayTag centerTag = loader.save(value.getBottomCenter());
        tag.put("center", centerTag);
        tag.putDouble("radius", value.getRadius());
        tag.putInt("height", value.getHeight());
        return tag;
    }

    public CylinderArea load(CompoundTag nbtElement) {
        Vec3iLoader loader = new Vec3iLoader();
        Vec3i center = loader.load((IntArrayTag) Objects.requireNonNull(nbtElement.get("center")));
        double radius = nbtElement.getDouble("radius");
        int height = nbtElement.getInt("height");
        return new CylinderArea(new BlockPos(center), height, radius);
    }

    public CompoundTag parse(String s) {
        return null;
    }

    public ConfigLoader<CompoundTag, BlockArea> asBlockAreaLoader() {
        return new ConfigLoader<>() {
            @Override
            public CompoundTag save(BlockArea value) {
                if (!(value instanceof CylinderArea cylinderArea))
                    throw new IllegalArgumentException("Invalid block area type");
                return CylinderAreaLoader.this.save(cylinderArea);
            }

            @Override
            public BlockArea load(CompoundTag nbtElement) {
                return CylinderAreaLoader.this.load(nbtElement);
            }

            @Override
            public CompoundTag parse(String s) {
                return CylinderAreaLoader.this.parse(s);
            }
        };
    }
}
