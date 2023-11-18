package at.haha007.edenclient.utils.area;

import at.haha007.edenclient.command.CommandManager;
import at.haha007.edenclient.utils.PlayerUtils;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import lombok.Getter;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.commands.arguments.coordinates.Coordinates;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class CylinderArea implements BlockArea {

    @Getter
    private final BlockPos bottomCenter;
    @Getter
    private final int height;
    private final double radiusSquared;
    @Getter
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
        Vec3i size = max.subtract(min);

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

    private static CylinderArea fromCommand(CommandContext<ClientSuggestionProvider> context, String keyCenter, String keyRadius, String keyHeight) {
        double radius = context.getArgument(keyRadius, Double.class);
        Coordinates center = context.getArgument(keyCenter, Coordinates.class);
        int height = context.getArgument(keyHeight, Integer.class);
        return new CylinderArea(center.getBlockPos(PlayerUtils.getPlayer().createCommandSourceStack()), height, radius);
    }

    public static RequiredArgumentBuilder<ClientSuggestionProvider, Coordinates> command(
            String keyCenter,
            String keyRadius,
            String keyHeight,
            BiConsumer<CommandContext<ClientSuggestionProvider>, CylinderArea> executor) {
        var center = CommandManager.argument(keyCenter, BlockPosArgument.blockPos());
        var radius = CommandManager.argument(keyRadius, DoubleArgumentType.doubleArg());
        var height = CommandManager.argument(keyHeight, IntegerArgumentType.integer());
        height.executes(c -> {
            CylinderArea area = fromCommand(c, keyCenter, keyRadius, keyHeight);
            executor.accept(c, area);
            return 1;
        });
        center.then(radius);
        radius.then(height);
        return center;
    }
}
