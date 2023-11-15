package at.haha007.edenclient.mods.pathfinder;

import at.haha007.edenclient.EdenClient;
import at.haha007.edenclient.annotations.Mod;
import at.haha007.edenclient.utils.ChatColor;
import at.haha007.edenclient.utils.PlayerUtils;
import at.haha007.edenclient.utils.Scheduler;
import at.haha007.edenclient.utils.Utils;
import at.haha007.edenclient.utils.config.ConfigSubscriber;
import baritone.api.BaritoneAPI;
import baritone.api.pathing.goals.GoalBlock;
import baritone.api.process.ICustomGoalProcess;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.commands.arguments.coordinates.Coordinates;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static at.haha007.edenclient.command.CommandManager.*;
import static at.haha007.edenclient.utils.PlayerUtils.sendModMessage;

@Mod(dependencies = Scheduler.class)
public class PathFinder {

    private record VectorWithRunnable(Vec3i vec3i, Runnable runnable) {

    }

    @ConfigSubscriber("false")
    private boolean enabled;

    private final ICustomGoalProcess customGoalProcess;
    private final List<VectorWithRunnable> positionsToVisit = Collections.synchronizedList(new ArrayList<>());

    public PathFinder() {
        registerCommand();

        Scheduler scheduler = EdenClient.getMod(Scheduler.class);

        customGoalProcess = BaritoneAPI.getProvider().getPrimaryBaritone().getCustomGoalProcess();

        applyBaritoneSettings();

        scheduler.runAsync(this::runPathFinding);
    }

    private void applyBaritoneSettings() {
        // can sprint, but DON'T break or place blocks
        BaritoneAPI.getSettings().allowSprint.value = true;
        BaritoneAPI.getSettings().allowBreak.value = false;
        BaritoneAPI.getSettings().allowPlace.value = false;
    }

    private void runPathFinding() {
        while (!Thread.interrupted()) {
            while (positionsToVisit.isEmpty() || !enabled) {
                Utils.sleep(500);
            }

            // set the goal
            VectorWithRunnable vectorWithRunnable = positionsToVisit.get(0);
            Vec3i goal = vectorWithRunnable.vec3i;
            Runnable runnable = vectorWithRunnable.runnable;
            customGoalProcess.setGoalAndPath(new GoalBlock(goal.getX(), goal.getY(), goal.getZ()));

            // await the end of the goal
            while (customGoalProcess.isActive()) {
                if (!enabled) { // need to cancel the pathfinder immediately!
                    LocalPlayer player = PlayerUtils.getPlayer();
                    // set the current position of the player as the current goal, so the baritone pathfinder terminates
                    customGoalProcess.setGoalAndPath(new GoalBlock(player.getBlockX(), player.getBlockY(), player.getBlockZ()));
                }
                Utils.sleep(100); // while it is running, check every 100 ms whether it's finished
            }

            if (enabled) {
                // reached goal, run the runnable
                runnable.run();

                // remove the goal at the end --> if it cancelled it is still kept in the list
                positionsToVisit.remove(0);
            }
        }
    }

    private void registerCommand() {
        var node = literal("epathfinder");

        node.then(literal("visit").then(argument("target", BlockPosArgument.blockPos()).executes(c -> {
            BlockPos pos = c.getArgument("target", Coordinates.class).getBlockPos(PlayerUtils.getPlayer().createCommandSourceStack());
            addPositionToVisit(new VectorWithRunnable(new Vec3i(pos.getX(), pos.getY(), pos.getZ()), null));
            return 1;
        })));

        node.then(literal("list").executes(c -> {
            sendList(5);
            return 1;
        }).then(argument("amount", IntegerArgumentType.integer(1, 100)).executes(c -> {
            int amount = c.getArgument("amount", Integer.class);
            sendList(amount);
            return 1;
        })));

        node.then(literal("toggle").executes(c -> {
            this.enabled = !enabled;
            sendModMessage(ChatColor.GOLD + ((enabled ? "Enabled" : "Disabled") + " PathFinder."));
            return 1;
        }));

        node.then(literal("clear").executes(c -> {
            sendModMessage(ChatColor.GOLD + "Clearing goals.");
            clear();
            return 1;
        }));

        register(node, "Pathfinder allows you to use baritone pathfinding efficiently.");
    }

    private void sendList(int amount) {
        sendModMessage(ChatColor.GOLD + "Next goals (total found: " + positionsToVisit.size() + ", sending up to: " + amount + ")");
        for (int i = 0; i < amount; i++) {
            if (positionsToVisit.size() > i) {
                Vec3i position = positionsToVisit.get(i).vec3i;
                sendModMessage(ChatColor.AQUA + String.format("[%6d, %3d, %6d]", position.getX(), position.getY(), position.getZ()));
            }
        }
    }

    public void addPositionToVisit(Vec3i position, Runnable runnable) {
        positionsToVisit.add(new VectorWithRunnable(position, runnable));
    }

    private void addPositionToVisit(VectorWithRunnable vectorWithRunnable) {
        positionsToVisit.add(vectorWithRunnable);
    }

    public boolean isRunning() {
        return !positionsToVisit.isEmpty() && enabled;
    }

    public void enable() {
        this.enabled = true;
    }

    public void clear() {
        this.positionsToVisit.clear();
    }

    public boolean isEnabled() {
        return enabled;
    }
}
