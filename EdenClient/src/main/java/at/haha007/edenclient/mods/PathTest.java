package at.haha007.edenclient.mods;

import at.haha007.edenclient.annotations.Mod;
import at.haha007.edenclient.callbacks.GameRenderCallback;
import at.haha007.edenclient.callbacks.PlayerTickCallback;
import at.haha007.edenclient.utils.EdenRenderUtils;
import at.haha007.edenclient.utils.PlayerUtils;
import at.haha007.edenclient.utils.Scheduler;
import at.haha007.edenclient.utils.pathing.PathFinder;
import at.haha007.edenclient.utils.pathing.PathRenderer;
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
import java.util.concurrent.atomic.AtomicLong;

import static at.haha007.edenclient.command.CommandManager.*;

@Mod(dependencies = Scheduler.class)
public class PathTest {
    private volatile PathSegment path;
    private PathFinder.PathSearch pathSearch;
    private Thread pathSearchThread;
    private volatile boolean pathSearchFinished;
    private final AtomicLong activeSearchGeneration = new AtomicLong();
    private boolean shouldRender = true;
    private TaskManager taskManager;

    public PathTest() {
        registerCommand();
        GameRenderCallback.EVENT.register(this::render, getClass());
        PlayerTickCallback.EVENT.register(this::tick, getClass());
    }

    private void tick(LocalPlayer player) {
        if (pathSearch == null) {
            return;
        }

        if (!pathSearchFinished) {
            return;
        }

        path = pathSearch.getResolvedPath();
        String message = switch (pathSearch.getStatus()) {
            case FOUND_EXACT -> "Path search finished with an exact path.";
            case FOUND_BEST_EFFORT -> "Path search finished with the best reachable path.";
            case FAILED -> "Path search failed to find an exact path.";
            case ALREADY_AT_TARGET -> "Already at the requested target.";
            case RUNNING -> null;
        };
        if (message != null) {
            PlayerUtils.sendModMessage(message);
        }
        pathSearch = null;
        pathSearchThread = null;
        pathSearchFinished = false;
    }

    private void render(float tickDelta) {
        if (!shouldRender) return;
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
//        StraightSegmentCalculator calculator = new StraightSegmentCalculator(3.5);
//        Collection<PathSegment> segments = calculator.calculateSegments(PlayerUtils.getPlayer().position().add(0, .01, 0));
//        for (PathSegment segment : segments) {
//            Vec3 vec3 = segment.to();
//            EdenRenderUtils.drawAreaOutline(vec3.add(-.1, .001, -.1), vec3.add(.1, .16, .1), Color4f.fromColor(Color.BLUE.getRGB()));
//        }


        if (path == null) return;
        PathRenderer.renderPath(path);
    }


    private void registerCommand() {
        LiteralArgumentBuilder<FabricClientCommandSource> node = literal("epathtest");

        node.executes(c -> {
            shouldRender = !shouldRender;
            PlayerUtils.sendModMessage(shouldRender ? "Enabled" : "Disabled");
            return 1;
        }).then(argument("distance", DoubleArgumentType.doubleArg(1)).executes(c -> {
            startPathTowards(c.getArgument("distance", Double.class));
            return 1;
        })).then(literal("clear").executes(c -> {
            path = null;
            stopPathSearch();
            return 1;
        })).then(literal("start").executes(c -> {
            if (path == null) {
                PlayerUtils.sendModMessage("No path generated yet. Use /epathtest <distance> to generate a path.");
                return 1;
            }
            if (pathSearch != null) {
                stopPathSearch();
                PlayerUtils.sendModMessage("Stopped the active search and started following the current best path.");
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
            if (taskManager != null) {
                taskManager.cancel();
                taskManager = null;
            }
            return 1;
        }));

        register(node, "/epathtest [<distance>,clear]");
    }

    private void startPathTowards(double distance) {
        stopPathSearch();
        Vec3 playerPos = PlayerUtils.getPlayer().position();
        Entity camera = Minecraft.getInstance().getCameraEntity();
        if (camera == null) {
            return;
        }
        Vec3 direction = camera.getLookAngle();
        direction = direction.normalize().scale(distance);
        Vec3 target = playerPos.add(direction.x(), direction.y(), direction.z());
        pathSearch = PathFinder.createDefault().startSearch(playerPos, target, false);
        path = pathSearch.getBestPathSoFar();
        startPathSearchThread(pathSearch);
        PlayerUtils.sendModMessage("Started incremental path search.");
    }

    private void startPathSearchThread(PathFinder.PathSearch search) {
        long searchGeneration = activeSearchGeneration.incrementAndGet();
        pathSearchFinished = false;
        pathSearchThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted() && searchGeneration == activeSearchGeneration.get() && !search.isDone()) {
                search.advance();
                path = search.getBestPathSoFar();
            }
            if (searchGeneration == activeSearchGeneration.get()) {
                pathSearchFinished = true;
            }
        }, "edenclient-path-search");
        pathSearchThread.setDaemon(true);
        pathSearchThread.start();
    }

    private void stopPathSearch() {
        activeSearchGeneration.incrementAndGet();
        pathSearchFinished = false;
        if (pathSearchThread != null) {
            pathSearchThread.interrupt();
            pathSearchThread = null;
        }
        pathSearch = null;
    }

}
