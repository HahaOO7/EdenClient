package at.haha007.edenclient.utils.area;

import at.haha007.edenclient.utils.config.loaders.ConfigLoader;
import at.haha007.edenclient.utils.config.loaders.Vec3iLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class CylinderAreaLoader implements ConfigLoader<CompoundTag, CylinderArea> {

    @NotNull
    public CompoundTag save(@NotNull CylinderArea value) {
        CompoundTag tag = new CompoundTag();
        Vec3iLoader loader = new Vec3iLoader();
        IntArrayTag centerTag = loader.save(value.getBottomCenter());
        tag.put("center", centerTag);
        tag.putDouble("radius", value.getRadius());
        tag.putInt("height", value.getHeight());
        return tag;
    }

    @NotNull
    public CylinderArea load(@NotNull CompoundTag nbtElement) {
        Vec3iLoader loader = new Vec3iLoader();
        Vec3i center = loader.load((IntArrayTag) Objects.requireNonNull(nbtElement.get("center")));
        double radius = nbtElement.getDouble("radius").orElseThrow();
        int height = nbtElement.getInt("height").orElseThrow();
        return new CylinderArea(new BlockPos(center), height, radius);
    }

    @NotNull
    public CompoundTag parse(@NotNull String s) {
        CompoundTag tag = new CompoundTag();
        tag.put("center", new Vec3iLoader().save(Vec3i.ZERO));
        tag.putDouble("radius", -1);
        tag.putInt("height", -1);
        return tag;
    }

    public ConfigLoader<CompoundTag, BlockArea> asBlockAreaLoader() {
        return new ConfigLoader<>() {
            @Override
            @NotNull
            public CompoundTag save(@NotNull BlockArea value) {
                if (!(value instanceof CylinderArea cylinderArea))
                    throw new IllegalArgumentException("Invalid block area type");
                return CylinderAreaLoader.this.save(cylinderArea);
            }

            @Override
            @NotNull
            public BlockArea load(@NotNull CompoundTag nbtElement) {
                return CylinderAreaLoader.this.load(nbtElement);
            }

            @Override
            @NotNull
            public CompoundTag parse(@NotNull String s) {
                return CylinderAreaLoader.this.parse(s);
            }
        };
    }
}
