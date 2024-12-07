package at.haha007.edenclient.utils.area;

import at.haha007.edenclient.command.CommandManager;
import at.haha007.edenclient.utils.PlayerUtils;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import dev.xpple.clientarguments.arguments.CBlockPosArgument;
import dev.xpple.clientarguments.arguments.CCoordinates;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.commands.arguments.coordinates.Coordinates;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class SphereArea implements BlockArea {
    private final BlockPos center;
    private final double radius;
    private final double radiusSquared;

    public SphereArea(BlockPos center, double radius) {
        this.center = center;
        this.radius = radius;
        this.radiusSquared = radius * radius;
    }

    public BlockPos getCenter() {
        return center;
    }

    public double getRadius() {
        return radius;
    }

    @Override
    public boolean contains(BlockPos pos) {
        return center.distSqr(pos) <= radiusSquared;
    }

    @Override
    public Stream<BlockPos> stream() {
        int delta = (int) Math.ceil(radius);
        Vec3i min = new Vec3i(center.getX() - delta, center.getY() - delta, center.getZ() - delta);
        Vec3i max = new Vec3i(center.getX() + delta, center.getY() + delta, center.getZ() + delta);
        BoundingBox bb = new BoundingBox(min.getX(), min.getY(), min.getZ(), max.getX(), max.getY(), max.getZ());
        return streamArea(bb).filter(this::contains);
    }

    @Override
    public Stream<BlockPos> wallStream() {
        //theoretically this should be sqrt(3)
        int delta = (int) Math.ceil(radius) + 2;
        Vec3i min = new Vec3i(center.getX() - delta, center.getY() - delta, center.getZ() - delta);
        Vec3i max = new Vec3i(center.getX() + delta, center.getY() + delta, center.getZ() + delta);
        BoundingBox bb = new BoundingBox(min.getX(), min.getY(), min.getZ(), max.getX(), max.getY(), max.getZ());
        return streamArea(bb).filter(this::isWall);
    }

    @Override
    public Stream<BlockPos> floorStream() {
        int delta = (int) Math.ceil(radius) + 2;
        Vec3i min = new Vec3i(center.getX() - delta, center.getY() - delta, center.getZ() - delta);
        Vec3i max = new Vec3i(center.getX() + delta, center.getY() + delta, center.getZ() + delta);
        BoundingBox bb = new BoundingBox(min.getX(), min.getY(), min.getZ(), max.getX(), max.getY(), max.getZ());
        return streamArea(bb).filter(this::isFloor);
    }

    @Override
    public Stream<BlockPos> ceilingStream() {
        int delta = (int) Math.ceil(radius) + 2;
        Vec3i min = new Vec3i(center.getX() - delta, center.getY() - delta, center.getZ() - delta);
        Vec3i max = new Vec3i(center.getX() + delta, center.getY() + delta, center.getZ() + delta);
        BoundingBox bb = new BoundingBox(min.getX(), min.getY(), min.getZ(), max.getX(), max.getY(), max.getZ());
        return streamArea(bb).filter(this::isCeiling);
    }

    private static SphereArea fromCommand(CommandContext<FabricClientCommandSource> context, String keyCenter, String keyRadius) {
        double radius = context.getArgument(keyRadius, Double.class);
        BlockPos center = CBlockPosArgument.getBlockPos(context, keyCenter);
        return new SphereArea(center, radius);
    }


    public static RequiredArgumentBuilder<FabricClientCommandSource, CCoordinates> command(
            String keyCenter,
            String keyRadius,
            BiConsumer<CommandContext<FabricClientCommandSource>, SphereArea> executor) {
        RequiredArgumentBuilder<FabricClientCommandSource, CCoordinates> center = CommandManager.argument(keyCenter, CBlockPosArgument.blockPos());
        RequiredArgumentBuilder<FabricClientCommandSource, Double> radius = CommandManager.argument(keyRadius, DoubleArgumentType.doubleArg());
        radius.executes(c -> {
            SphereArea area = fromCommand(c, keyCenter, keyRadius);
            executor.accept(c, area);
            return 1;
        });
        center.then(radius);
        return center;
    }


    private Stream<BlockPos> streamArea(BoundingBox box) {
        Vec3i min = new Vec3i(box.minX(), box.minY(), box.minZ());
        Vec3i max = new Vec3i(box.maxX(), box.maxY(), box.maxZ());
        Vec3i size = max.subtract(min).offset(1, 1, 1);

        Stream<BlockPos> stream =  Stream.generate(new Supplier<>() {
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
}
