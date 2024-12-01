package at.haha007.edenclient.mods;

import at.haha007.edenclient.EdenClient;
import at.haha007.edenclient.annotations.Mod;
import at.haha007.edenclient.render.CubeRenderer;
import at.haha007.edenclient.render.TracerRenderer;
import at.haha007.edenclient.utils.PlayerUtils;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.xpple.clientarguments.arguments.CBlockPosArgument;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
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
        LiteralArgumentBuilder<FabricClientCommandSource> cmd = literal("erendershape");
        LiteralArgumentBuilder<FabricClientCommandSource> boxCmd = literal("box");
        boxCmd.then(argument("pos1", new CBlockPosArgument())
                .then(argument("pos2", new CBlockPosArgument()).then(argument("time", IntegerArgumentType.integer(1))
                        .executes(c -> {
                            BlockPos pos1 = CBlockPosArgument.getBlockPos(c, "pos1");
                            BlockPos pos2 = CBlockPosArgument.getBlockPos(c, "pos2");
                            int t = c.getArgument("time", Integer.class) * 20;
                            EdenClient.getMod(CubeRenderer.class).add(AABB.of(BoundingBox.fromCorners(pos1, pos2)), t);
                            return 1;
                        }))));
        LiteralArgumentBuilder<FabricClientCommandSource> tracerCmd = literal("tracer");
        tracerCmd.then(argument("target", BlockPosArgument.blockPos()).then(argument("time", IntegerArgumentType.integer(1))
                .executes(c -> {
                    int time = c.getArgument("time", Integer.class) * 20;
                    BlockPos target = c.getArgument("target", Coordinates.class).getBlockPos(PlayerUtils.getPlayer().createCommandSourceStackForNameResolution(null));
                    EdenClient.getMod(TracerRenderer.class).add(Vec3.atCenterOf(target), time);
                    return 1;
                })));

        cmd.then(boxCmd);
        cmd.then(tracerCmd);

        register(cmd, "RenderShape is only for internal use.");
    }
}
