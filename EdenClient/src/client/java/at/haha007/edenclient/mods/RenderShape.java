package at.haha007.edenclient.mods;

import at.haha007.edenclient.EdenClient;
import at.haha007.edenclient.annotations.Mod;
import at.haha007.edenclient.callbacks.GameRenderCallback;
import at.haha007.edenclient.render.CubeRenderer;
import at.haha007.edenclient.render.EdenColor;
import at.haha007.edenclient.render.TracerRenderer;
import at.haha007.edenclient.utils.EdenRenderUtils;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.xpple.clientarguments.arguments.CBlockPosArgument;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import static at.haha007.edenclient.command.CommandManager.*;

@Mod(dependencies = {TracerRenderer.class})
public class RenderShape {
    private final CubeRenderer cubeRenderer = new CubeRenderer();

    public RenderShape() {
        cubeRenderer.settings()
                .facesEnabled(true)
                .faceColor(new EdenColor(1, 0, 0, 0.5f))
                .faceDepth(EdenRenderUtils.DepthTest.DEPTH_TESTED)
                .edgeColor(new EdenColor(0, 1, 0, 1))
                .edgeDepth(EdenRenderUtils.DepthTest.NO_DEPTH)
                .edgeWidth(5);
        LiteralArgumentBuilder<FabricClientCommandSource> cmd = literal("erendershape");
        LiteralArgumentBuilder<FabricClientCommandSource> boxCmd = literal("box");
        boxCmd.then(argument("pos1", new CBlockPosArgument())
                .then(argument("pos2", new CBlockPosArgument())
                        .executes(c -> {
                            BlockPos pos1 = CBlockPosArgument.getBlockPos(c, "pos1");
                            BlockPos pos2 = CBlockPosArgument.getBlockPos(c, "pos2");
                            cubeRenderer.add(AABB.of(BoundingBox.fromCorners(pos1, pos2)));
                            return 1;
                        })));
        boxCmd.then(literal("clear")
                .executes(c -> {
                    EdenClient.getMod(CubeRenderer.class).clear();
                    return 1;
                }));
        LiteralArgumentBuilder<FabricClientCommandSource> tracerCmd = literal("tracer");
        tracerCmd.then(argument("target", CBlockPosArgument.blockPos()).then(argument("time", IntegerArgumentType.integer(1))
                .executes(c -> {
                    int time = c.getArgument("time", Integer.class) * 20;
                    BlockPos target = CBlockPosArgument.getBlockPos(c, "target");
                    EdenClient.getMod(TracerRenderer.class).add(Vec3.atCenterOf(target), time);
                    return 1;
                })));

        cmd.then(boxCmd);
        cmd.then(tracerCmd);

        register(cmd, "RenderShape is only for internal use.");

        GameRenderCallback.EVENT.register(this::render, getClass());
    }

    private void render(float v) {
        cubeRenderer.render(v);
    }
}
