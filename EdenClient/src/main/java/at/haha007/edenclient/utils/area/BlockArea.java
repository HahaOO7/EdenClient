package at.haha007.edenclient.utils.area;

import at.haha007.edenclient.command.CommandManager;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.core.BlockPos;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

public interface BlockArea {

    static List<ArgumentBuilder<ClientSuggestionProvider, ?>> commands(BiConsumer<CommandContext<ClientSuggestionProvider>, BlockArea> executor) {
        ArgumentBuilder<ClientSuggestionProvider, ?> cube = CubeArea.command("corner1", "corner2", executor::accept);
        ArgumentBuilder<ClientSuggestionProvider, ?> cylinder = CylinderArea.command("bottomCenter", "radius", "height", executor::accept);
        ArgumentBuilder<ClientSuggestionProvider, ?> sphere = SphereArea.command("center", "radius", executor::accept);
        cube = CommandManager.literal("cube").then(cube);
        cylinder = CommandManager.literal("cylinder").then(cylinder);
        sphere = CommandManager.literal("sphere").then(sphere);
        return List.of(cube, cylinder, sphere);
    }


    boolean contains(BlockPos pos);

    default boolean isWall(BlockPos pos) {
        if (contains(pos)) return false;
        return contains(pos.north()) ||
                contains(pos.east()) ||
                contains(pos.south()) ||
                contains(pos.west());
    }

    default boolean isFloor(BlockPos pos) {
        if (contains(pos)) return false;
        return contains(pos.above());
    }

    default boolean isCeiling(BlockPos pos) {
        if (contains(pos)) return false;
        return contains(pos.below());
    }

    Stream<BlockPos> stream();

    Stream<BlockPos> wallStream();

    Stream<BlockPos> floorStream();

    Stream<BlockPos> ceilingStream();


}
