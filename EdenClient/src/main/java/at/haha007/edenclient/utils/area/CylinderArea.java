package at.haha007.edenclient.utils.area;

import at.haha007.edenclient.command.CommandManager;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import dev.xpple.clientarguments.arguments.CBlockPosArgument;
import dev.xpple.clientarguments.arguments.CCoordinates;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class CylinderArea implements BlockArea {

    private final BlockPos bottomCenter;
    private final int height;
    private final double radiusSquared;
    private final double radius;

    public CylinderArea(BlockPos bottomCenter, int height, double radius) {
        this.bottomCenter = bottomCenter;
        this.height = height;
        this.radius = radius;
        this.radiusSquared = radius * radius;
    }

    public CylinderArea(int x, int z, int minY, int maxY, double radius) {
        this(new BlockPos(x, minY, z), maxY - minY, radius);
    }

    public double getRadius() {
        return radius;
    }

    public BlockPos getBottomCenter() {
        return bottomCenter;
    }

    public int getHeight() {
        return height;
    }

    @Override
    public boolean contains(BlockPos pos) {
        int y = pos.getY();
        if (y < bottomCenter.getY() || y > bottomCenter.getY() + height) {
            return false;
        }

        int dx = pos.getX() - bottomCenter.getX();
        int dz = pos.getZ() - bottomCenter.getZ();
        return dx * dx + dz * dz <= radiusSquared;
    }

    @Override
    public Stream<BlockPos> stream() {
        int delta = (int) Math.ceil(radius) + 2;
        Vec3i min = new Vec3i(bottomCenter.getX() - delta, bottomCenter.getY(), bottomCenter.getZ() - delta);
        Vec3i max = new Vec3i(bottomCenter.getX() + delta, bottomCenter.getY() + height, bottomCenter.getZ() + delta);
        BoundingBox bb = new BoundingBox(min.getX(), min.getY(), min.getZ(), max.getX(), max.getY(), max.getZ());
        return streamArea(bb).filter(this::contains);
    }

    @Override
    public Stream<BlockPos> wallStream() {
        int delta = (int) Math.ceil(radius) + 3;
        Vec3i min = new Vec3i(bottomCenter.getX() - delta, bottomCenter.getY(), bottomCenter.getZ() - delta);
        Vec3i max = new Vec3i(bottomCenter.getX() + delta, bottomCenter.getY() + height, bottomCenter.getZ() + delta);
        BoundingBox bb = new BoundingBox(min.getX(), min.getY(), min.getZ(), max.getX(), max.getY(), max.getZ());
        return streamArea(bb).filter(this::isWall);
    }

    @Override
    public Stream<BlockPos> floorStream() {
        int y = bottomCenter.getY() - 1;
        int delta = (int) Math.ceil(radius) + 2;
        Vec3i min = new Vec3i(bottomCenter.getX() - delta, y, bottomCenter.getZ() - delta);
        Vec3i max = new Vec3i(bottomCenter.getX() + delta, y, bottomCenter.getZ() + delta);
        BoundingBox bb = new BoundingBox(min.getX(), min.getY(), min.getZ(), max.getX(), max.getY(), max.getZ());
        return streamArea(bb).filter(this::isFloor);
    }

    @Override
    public Stream<BlockPos> ceilingStream() {
        int y = bottomCenter.getY() + height;
        int delta = (int) Math.ceil(radius) + 2;
        Vec3i min = new Vec3i(bottomCenter.getX() - delta, y, bottomCenter.getZ() - delta);
        Vec3i max = new Vec3i(bottomCenter.getX() + delta, y, bottomCenter.getZ() + delta);
        BoundingBox bb = new BoundingBox(min.getX(), min.getY(), min.getZ(), max.getX(), max.getY(), max.getZ());
        return streamArea(bb).filter(this::isCeiling);
    }


    private Stream<BlockPos> streamArea(BoundingBox box) {
        Vec3i min = new Vec3i(box.minX(), box.minY(), box.minZ());
        Vec3i max = new Vec3i(box.maxX(), box.maxY(), box.maxZ());
        Vec3i size = max.subtract(min).offset(1, 1, 1);

        Stream<BlockPos> stream = Stream.generate(new Supplier<>() {
            private long i = 0;

            public BlockPos get() {
                int x = (int) (i % size.getX());
                int y = (int) ((i / size.getX()) % size.getY());
                int z = (int) ((i / size.getX() / size.getY()) % size.getZ());
                if (z > size.getZ())
                    return null;
                BlockPos pos = new BlockPos(x, y, z).offset(min);
                i++;
                return pos;
            }
        });
        return stream.limit((long) box.getXSpan() * box.getYSpan() * box.getZSpan()).filter(Objects::nonNull);
    }

    private static CylinderArea fromCommand(CommandContext<FabricClientCommandSource> context, String keyCenter, String keyRadius, String keyHeight) {
        double radius = DoubleArgumentType.getDouble(context, keyRadius);
        BlockPos center = CBlockPosArgument.getBlockPos(context, keyCenter);
        int height = IntegerArgumentType.getInteger(context, keyHeight);
        return new CylinderArea(center, height, radius);
    }

    public static RequiredArgumentBuilder<FabricClientCommandSource, CCoordinates> command(
            String keyCenter,
            String keyRadius,
            String keyHeight,
            BiConsumer<CommandContext<FabricClientCommandSource>, CylinderArea> executor) {
        RequiredArgumentBuilder<FabricClientCommandSource, CCoordinates> center = CommandManager.argument(keyCenter, CBlockPosArgument.blockPos());
        RequiredArgumentBuilder<FabricClientCommandSource, Double> radius = CommandManager.argument(keyRadius, DoubleArgumentType.doubleArg(1));
        RequiredArgumentBuilder<FabricClientCommandSource, Integer> height = CommandManager.argument(keyHeight, IntegerArgumentType.integer(1));

        height.executes(c -> {
            CylinderArea area = fromCommand(c, keyCenter, keyRadius, keyHeight);
            executor.accept(c, area);
            return 1;
        });
        radius.then(height);
        center.then(radius);
        return center;
    }
}
