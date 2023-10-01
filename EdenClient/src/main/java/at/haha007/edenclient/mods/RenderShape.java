package at.haha007.edenclient.mods;

import at.haha007.edenclient.annotations.Mod;
import at.haha007.edenclient.EdenClient;
import at.haha007.edenclient.render.CubeRenderer;
import at.haha007.edenclient.render.TracerRenderer;
import at.haha007.edenclient.utils.PlayerUtils;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.commands.arguments.coordinates.Coordinates;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import static at.haha007.edenclient.command.CommandManager.*;

@Mod
public class RenderShape {

    public RenderShape() {
        LiteralArgumentBuilder<ClientSuggestionProvider> cmd = literal("erendershape");
        LiteralArgumentBuilder<ClientSuggestionProvider> boxCmd = literal("box");
        boxCmd.then(argument("pos1", new BlockPosArgument())
                .then(argument("pos2", new BlockPosArgument()).then(argument("time", IntegerArgumentType.integer(1))
                        .executes(c -> {
                            BlockPos pos1 = c.getArgument("pos1", Coordinates.class).getBlockPos(PlayerUtils.getPlayer().createCommandSourceStack());
                            BlockPos pos2 = c.getArgument("pos2", Coordinates.class).getBlockPos(PlayerUtils.getPlayer().createCommandSourceStack());
                            int t = c.getArgument("time", Integer.class) * 20;
                            EdenClient.getMod(CubeRenderer.class).add(AABB.of(BoundingBox.fromCorners(pos1, pos2)), t);
                            return 1;
                        }))));
        LiteralArgumentBuilder<ClientSuggestionProvider> tracerCmd = literal("tracer");
        tracerCmd.then(argument("target", BlockPosArgument.blockPos()).then(argument("time", IntegerArgumentType.integer(1))
                .executes(c -> {
                    int time = c.getArgument("time", Integer.class) * 20;
                    BlockPos target = c.getArgument("target", Coordinates.class).getBlockPos(PlayerUtils.getPlayer().createCommandSourceStack());
                    EdenClient.getMod(TracerRenderer.class).add(Vec3.atCenterOf(target), time);
                    return 1;
                })));

        cmd.then(boxCmd);
        cmd.then(tracerCmd);

        register(cmd,
                "RenderShape is only for internal use.");
    }
}
