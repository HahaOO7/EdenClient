package at.haha007.edenclient.utils.area;

import at.haha007.edenclient.command.CommandManager;
import at.haha007.edenclient.utils.PlayerUtils;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import lombok.Getter;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.commands.arguments.coordinates.Coordinates;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

@Getter
public class CubeArea implements BlockArea {
    private final BoundingBox box;

    public CubeArea(@NotNull BoundingBox box) {
        this.box = box;
    }

    public CubeArea(Vec3i min, Vec3i max) {
        this(new BoundingBox(min.getX(), min.getY(), min.getZ(), max.getX(), max.getY(), max.getZ()));
    }

    @Override
    public boolean contains(BlockPos pos) {
        return box.isInside(pos);
    }

    @Override
    public Stream<BlockPos> stream() {
        return streamArea(box);
    }

    @Override
    public Stream<BlockPos> wallStream() {
        Vec3i min = new Vec3i(box.minX(), box.minY(), box.minZ());
        Vec3i max = new Vec3i(box.maxX(), box.maxY(), box.maxZ());
        BoundingBox north = new BoundingBox(min.getX(), min.getY(), min.getZ() - 1, max.getX(), max.getY(), min.getZ() - 1);
        BoundingBox south = new BoundingBox(min.getX(), min.getY(), max.getZ() + 1, max.getX(), max.getY(), max.getZ() + 1);
        BoundingBox east = new BoundingBox(max.getX() + 1, min.getY(), min.getZ(), max.getX() + 1, max.getY(), max.getZ());
        BoundingBox west = new BoundingBox(min.getX() - 1, min.getY(), min.getZ(), min.getX() - 1, max.getY(), max.getZ());
        return Stream.concat(
                Stream.concat(
                        streamArea(north),
                        streamArea(south)
                ),
                Stream.concat(
                        streamArea(west),
                        streamArea(east)
                )
        );
    }

    @Override
    public Stream<BlockPos> floorStream() {
        Vec3i min = new Vec3i(box.minX(), box.minY() - 1, box.minZ());
        Vec3i max = new Vec3i(box.maxX(), box.minY() - 1, box.maxZ());
        return streamArea(new BoundingBox(min.getX(), min.getY(), min.getZ(), max.getX(), max.getY(), max.getZ()));
    }

    @Override
    public Stream<BlockPos> ceilingStream() {
        Vec3i min = new Vec3i(box.minX(), box.maxY() + 1, box.minZ());
        Vec3i max = new Vec3i(box.maxX(), box.maxY() + 1, box.maxZ());
        return streamArea(new BoundingBox(min.getX(), min.getY(), min.getZ(), max.getX(), max.getY(), max.getZ()));
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

    /**
     * parses a CubeArea from a command
     *
     * @param context The CommandContext with the provided positions
     * @param key1    the key of the first position
     * @param key2    the key of the second position
     * @return the parsed CubeArea
     */
    private static CubeArea fromCommand(CommandContext<ClientSuggestionProvider> context, String key1, String key2) {
        Coordinates pos1 = context.getArgument(key1, Coordinates.class);
        Coordinates pos2 = context.getArgument(key2, Coordinates.class);
        CommandSourceStack stack = PlayerUtils.getPlayer().createCommandSourceStack();
        return new CubeArea(pos1.getBlockPos(stack), pos2.getBlockPos(stack));

    }

    /**
     * Generates a RequiredArgumentBuilder object for a command with two position arguments.
     *
     * @param pos1     the name of the first position argument
     * @param pos2     the name of the second position argument
     * @param executor a consumer that accepts a CommandContext object and performs some action
     * @return the generated RequiredArgumentBuilder object
     */
    public static RequiredArgumentBuilder<ClientSuggestionProvider, Coordinates> command(String pos1, String pos2, BiConsumer<CommandContext<ClientSuggestionProvider>, CubeArea> executor) {
        RequiredArgumentBuilder<ClientSuggestionProvider, Coordinates> a = CommandManager.argument(pos1, BlockPosArgument.blockPos());
        RequiredArgumentBuilder<ClientSuggestionProvider, Coordinates> b = CommandManager.argument(pos2, BlockPosArgument.blockPos());
        b = b.executes(c -> {
            CubeArea area = fromCommand(c, pos1, pos2);
            executor.accept(c, area);
            return 1;
        });
        a = a.then(b);
        return a;
    }
}
