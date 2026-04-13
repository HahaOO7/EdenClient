package at.haha007.edenclient.mods;

import at.haha007.edenclient.annotations.Mod;
import at.haha007.edenclient.callbacks.GameRenderCallback;
import at.haha007.edenclient.callbacks.PlayerTickCallback;
import at.haha007.edenclient.utils.EdenRenderUtils;
import at.haha007.edenclient.utils.PlayerUtils;
import at.haha007.edenclient.utils.Scheduler;
import at.haha007.edenclient.utils.pathing.PathFinder;
import at.haha007.edenclient.utils.pathing.PathRenderer;
import at.haha007.edenclient.utils.pathing.segment.MasterPathSegment;
import at.haha007.edenclient.utils.pathing.segment.PathSegment;
import at.haha007.edenclient.utils.pathing.segmentcalculator.StraightSegmentCalculator;
import at.haha007.edenclient.utils.tasks.Task;
import at.haha007.edenclient.utils.tasks.TaskManager;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import fi.dy.masa.malilib.util.data.Color4f;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.Collection;

import static at.haha007.edenclient.command.CommandManager.*;

@Mod(dependencies = Scheduler.class)
public class PathTest {
    private MasterPathSegment path;
    private boolean enabled = false;
    private TaskManager taskManager;

    public PathTest() {
        registerCommand();
        GameRenderCallback.EVENT.register(this::render, getClass());
        PlayerTickCallback.EVENT.register(this::tick, getClass());
    }

    private void tick(LocalPlayer player) {
    }

    private void render(float tickDelta) {
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        StraightSegmentCalculator calculator = new StraightSegmentCalculator(3.5);
        Collection<PathSegment> segments = calculator.calculateSegments(PlayerUtils.getPlayer().position().add(0, .01, 0));
        for (PathSegment segment : segments) {
            Vec3 vec3 = segment.to();
            EdenRenderUtils.drawAreaOutline(vec3.add(-.5, .001, -.5), vec3.add(.5, .16, .5), Color4f.fromColor(Color.BLUE.getRGB()));
        }

//        BlockPos playerPos = PlayerUtils.getPlayer().getBlockPosBelowThatAffectsMyMovement();
//        for (int x = -5; x <= 5; x++) {
//            for (int z = -5; z <= 5; z++) {
//                for (int y = -5; y <= 2; y++) {
//                    Optional<Double> height = PathingUtils.getWalkableHeight(playerPos.offset(x, y, z));
//                    if(height.isEmpty()) continue;
//
//                    Vec3 min = Vec3.atLowerCornerOf(playerPos.offset(x, -playerPos.getY(), z)).add(0, height.get(), 0);
//
//                    Vec3 max = min.add(1, 0.16, 1);
//                    EdenRenderUtils.drawAreaOutline(min, max, Color4f.fromColor(Color.RED.getRGB()));
//                }
//            }
//        }


        if (path == null) return;
        PathRenderer.renderPath(path.children(), tickDelta);
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
        })).then(literal("start").executes(c -> {
            if (path == null) {
                PlayerUtils.sendModMessage("No path generated yet. Use /epathtest <distance> to generate a path.");
                return 1;
            }
            if (taskManager != null) {
                taskManager.cancel();
            }
            Task follower = path.follower();
            taskManager = new TaskManager();
            taskManager.then(follower);
            taskManager.then(() -> taskManager = null);
            taskManager.start();
            return 1;
        })).then(literal("stop").executes(c -> {
            taskManager.cancel();
            return 1;
        }));

        register(node, "/epathtest [<distance>,clear]");
    }

    private void generatePathTowards(double distance) {
        Vec3 playerPos = PlayerUtils.getPlayer().position();
        Entity camera = Minecraft.getInstance().getCameraEntity();
        if (camera == null) {
            return;
        }
        Vec3 direction = camera.getLookAngle();
        direction = direction.normalize().scale(distance);
        Vec3 target = playerPos.add(direction.x(), direction.y(), direction.z());
        MasterPathSegment tempPath = (MasterPathSegment) PathFinder.createDefault().findPath(playerPos, target, false);
        path = tempPath == null ? path : tempPath;
    }

}
