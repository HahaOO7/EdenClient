package at.haha007.edenclient.mods;

import at.haha007.edenclient.annotations.Mod;
import at.haha007.edenclient.callbacks.GameRenderCallback;
import at.haha007.edenclient.callbacks.PlayerTickCallback;
import at.haha007.edenclient.utils.PlayerUtils;
import at.haha007.edenclient.utils.Scheduler;
import at.haha007.edenclient.utils.pathing.Path;
import at.haha007.edenclient.utils.pathing.PathFinder;
import at.haha007.edenclient.utils.pathing.PathRenderer;
import at.haha007.edenclient.utils.pathing.SimplePathFinder;
import at.haha007.edenclient.utils.tasks.TaskManager;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.opengl.GL11;

import static at.haha007.edenclient.command.CommandManager.*;

@Mod(dependencies = Scheduler.class)
public class PathTest {
    private Path path;
    private boolean enabled = false;

    public PathTest() {
        registerCommand();
        GameRenderCallback.EVENT.register(this::render, getClass());
        PlayerTickCallback.EVENT.register(this::tick, getClass());
        TaskManager tm = new TaskManager();
        tm.then(() -> {
            while (true) {
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
                LocalPlayer localPlayer = Minecraft.getInstance().player;
                if (localPlayer == null) continue;
                Vec3 target = localPlayer.position().add(localPlayer.getLookAngle().scale(100));
                path = PathFinder.createDefault()
                        .findPath(localPlayer.blockPosition(), new BlockPos((int) target.x, (int) target.y, (int) target.z), false);
            }
        });
        tm.then(tm::start);
        tm.start();
    }

    private void tick(LocalPlayer localPlayer) {
//        if (path == null) return;
//        PathPosition nearest = path.getNearest(localPlayer.position());
//        int segment = nearest.block();
//        if ((segment > 0 || path.getBlock(segment).endPos().distanceToSqr(localPlayer.position()) < .0001)
//                && nearest.distance() < .5
//                && path.length() > 1) {
//            path = path.subPath(1);
//        }
//
//        PathFollower.follow(path);
    }

    private void render(float tickDelta) {
//        Collection<BlockPos> validNeighbors = new StepDownNeighborCalculator(3)
//                .getValidNeighbors(Minecraft.getInstance().player.blockPosition());
//        for (BlockPos validNeighbor : validNeighbors) {
//            EdenRenderUtils.drawAreaOutline(Vec3.atLowerCornerOf(validNeighbor), Vec3.atLowerCornerWithOffset(validNeighbor, 1, 1, 1),
//                    new Color4f(1f, 0, 0, 1f));
//        }
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        if (path == null) return;
        PathRenderer.renderPath(path, tickDelta);
    }


    private void registerCommand() {
        LiteralArgumentBuilder<FabricClientCommandSource> node = literal("epathtest");

        node.executes(c -> {
            enabled = !enabled;
            PlayerUtils.sendModMessage(enabled ? "Enabled" : "Disabled");
            return 1;
        }).then(argument("distance", DoubleArgumentType.doubleArg(1)).executes(c -> {
            generatePathTowards(c.getArgument("distance", Double.class));
            return 1;
        })).then(literal("clear").executes(c -> {
            path = null;
            return 1;
        }));

        register(node);
    }

    private void generatePathTowards(double distance) {
        BlockPos playerPos = PlayerUtils.getPlayer().blockPosition();
        Vec3 direction = Minecraft.getInstance().getCameraEntity().getLookAngle();
        direction = direction.multiply(1, 0, 1).normalize().scale(distance);
        BlockPos target = playerPos.offset((int) direction.x(), (int) direction.y(), (int) direction.z());
        Path tempPath = SimplePathFinder.findPath(playerPos, target);
        path = tempPath == null ? path : tempPath;
    }

}
