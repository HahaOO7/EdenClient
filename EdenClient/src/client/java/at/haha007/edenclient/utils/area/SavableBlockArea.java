package at.haha007.edenclient.utils.area;

import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.NotNull;

import java.util.stream.Stream;

public class SavableBlockArea implements BlockArea {
    private final BlockArea area;
    private final BlockAreaType type;

    public SavableBlockArea(@NotNull BlockArea area) {
        Class<? extends BlockArea> clazz = area.getClass();
        if (clazz == CubeArea.class) this.type = BlockAreaType.CUBE;
        else if (clazz == CylinderArea.class) this.type = BlockAreaType.CYLINDER;
        else if (clazz == SphereArea.class) this.type = BlockAreaType.SPHERE;
        else throw new IllegalArgumentException("Unknown area type");
        this.area = area;
    }

    public BlockArea getArea() {
        return area;
    }

    public BlockAreaType getType() {
        return type;
    }

    @Override
    public boolean contains(BlockPos pos) {
        return area.contains(pos);
    }

    @Override
    public Stream<BlockPos> stream() {
        return area.stream();
    }

    @Override
    public Stream<BlockPos> wallStream() {
        return area.wallStream();
    }

    @Override
    public Stream<BlockPos> floorStream() {
        return area.floorStream();
    }

    @Override
    public Stream<BlockPos> ceilingStream() {
        return area.ceilingStream();
    }

    @Override
    public BlockPos center() {
        return area.center();
    }

    public enum BlockAreaType {
        CUBE, CYLINDER, SPHERE
    }

    @Override
    public String toString() {
        return area.toString();
    }
}
