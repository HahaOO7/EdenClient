package at.haha007.edenclient.mods;

import at.haha007.edenclient.EdenClient;
import at.haha007.edenclient.utils.PlayerUtils;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.client.network.ClientCommandSource;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.command.argument.PosArgument;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import static at.haha007.edenclient.command.CommandManager.*;
import static at.haha007.edenclient.utils.TextUtils.createGoldText;

public class RenderShape {

    public RenderShape() {
        LiteralArgumentBuilder<ClientCommandSource> cmd = literal("erendershape");
        LiteralArgumentBuilder<ClientCommandSource> boxCmd = literal("box");
        boxCmd.then(argument("pos1", new BlockPosArgumentType())
                .then(argument("pos2", new BlockPosArgumentType()).then(argument("time", IntegerArgumentType.integer(1))
                        .executes(c -> {
                            BlockPos pos1 = c.getArgument("pos1", PosArgument.class).toAbsoluteBlockPos(PlayerUtils.getPlayer().getCommandSource());
                            BlockPos pos2 = c.getArgument("pos2", PosArgument.class).toAbsoluteBlockPos(PlayerUtils.getPlayer().getCommandSource());
                            int t = c.getArgument("time", Integer.class) * 20;
                            EdenClient.INSTANCE.getCubeRenderer().add(Box.from(BlockBox.create(pos1, pos2)), t);
                            return 1;
                        }))));
        LiteralArgumentBuilder<ClientCommandSource> tracerCmd = literal("tracer");
        tracerCmd.then(argument("target", BlockPosArgumentType.blockPos()).then(argument("time", IntegerArgumentType.integer(1))
                .executes(c -> {
                    int time = c.getArgument("time", Integer.class) * 20;
                    BlockPos target = c.getArgument("target", PosArgument.class).toAbsoluteBlockPos(PlayerUtils.getPlayer().getCommandSource());
                    EdenClient.INSTANCE.getTracerRenderer().add(Vec3d.ofCenter(target), time);
                    return 1;
                })));

        cmd.then(boxCmd);
        cmd.then(tracerCmd);

        register(cmd,
                createGoldText("RenderShape is only for internal use."));
    }
}
