package at.haha007.edenclient.mods;

import at.haha007.edenclient.annotations.Mod;
import at.haha007.edenclient.callbacks.GameRenderCallback;
import at.haha007.edenclient.callbacks.PlayerTickCallback;
import at.haha007.edenclient.utils.PlayerUtils;
import at.haha007.edenclient.utils.area.BlockArea;
import at.haha007.edenclient.utils.area.BlockAreaRenderFactory;
import at.haha007.edenclient.utils.area.SavableBlockArea;
import at.haha007.edenclient.utils.area.SavableBlockAreaList;
import at.haha007.edenclient.utils.config.ConfigSubscriber;
import at.haha007.edenclient.utils.config.PerWorldConfig;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.minecraft.client.player.LocalPlayer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static at.haha007.edenclient.command.CommandManager.literal;
import static at.haha007.edenclient.command.CommandManager.register;

@Mod
public class Timer {
    private boolean enabled = false;
    private long startTime = 0;
    private List<Long> stepTimes = new ArrayList<>();

    public Timer() {
        PerWorldConfig.get().register(this, "timer");
        PlayerTickCallback.EVENT.register(this::tick, getClass());
        registerCommand();
    }

    private void registerCommand() {
        LiteralArgumentBuilder<FabricClientCommandSource> command = literal("etimer");

        command.then(literal("start").executes(c -> {
            if (enabled) {
                PlayerUtils.sendModMessage("Timer is already started.");
                return 1;
            }
            enabled = true;
            startTime = System.currentTimeMillis();
            PlayerUtils.sendModMessage("Timer started.");
            return 1;
        }));

        command.then(literal("step").executes(c -> {
            if (!enabled) {
                PlayerUtils.sendModMessage("Timer is not started. Use /etimer start to start the timer.");
                return 1;
            }
            long currentTime = System.currentTimeMillis();
            long totalTime = currentTime - startTime;
            long deltaTime = stepTimes.isEmpty() ? totalTime : totalTime - stepTimes.getLast();
            stepTimes.add(totalTime);
            Component message = Component.empty().append(Component.text("Step " + stepTimes.size() + ": ", NamedTextColor.DARK_AQUA))
                    .append(Component.text(formatDeltaTime(deltaTime), NamedTextColor.GOLD, TextDecoration.BOLD))
                    .append(Component.text(" [" + formatDeltaTime(totalTime) + "]", NamedTextColor.YELLOW));
            PlayerUtils.sendModMessage(message);
            return 1;
        }));

        command.then(literal("stop").executes(c -> {
            if (!enabled) {
                PlayerUtils.sendModMessage("Timer is not started. Use /etimer start to start the timer.");
                return 1;
            }
            long currentTime = System.currentTimeMillis();
            long deltaTime = currentTime - startTime;
            stepTimes.add(deltaTime);

            // Display all step times with total time and step deltas
            PlayerUtils.sendModMessage("Timer stopped.");
            for (int i = 0; i < stepTimes.size(); i++) {
                long stepTime = stepTimes.get(i);
                String stepString = formatDeltaTime(stepTime);
                String stepDeltaString;
                if (i == 0) {
                    stepDeltaString = stepString;
                } else {
                    long stepDelta = stepTime - stepTimes.get(i - 1);
                    stepDeltaString = formatDeltaTime(stepDelta);
                }
                Component message = Component.empty().append(Component.text("Step " + (i + 1) + ": ", NamedTextColor.DARK_AQUA))
                        .append(Component.text(stepDeltaString, NamedTextColor.GOLD, TextDecoration.BOLD))
                        .append(Component.text(" [" + stepString + "]", NamedTextColor.YELLOW));
                PlayerUtils.sendMessage(message);
            }
            enabled = false;
            stepTimes.clear();
            return 1;
        }));

        register(command, "Timer [start,step,stop]", "Displays the elapsed time since starting the timer.");
    }

    private void tick(LocalPlayer localPlayer) {
        if (!enabled) return;
        long currentTime = System.currentTimeMillis();
        long deltaTime = currentTime - startTime;
        String stepString = formatDeltaTime(deltaTime);
        String stepDeltaString;
        if (stepTimes.isEmpty()) {
            stepDeltaString = stepString;
        } else {
            long lastStepTime = stepTimes.getLast();
            long stepDelta = deltaTime - lastStepTime;
            stepDeltaString = formatDeltaTime(stepDelta);
        }
        PlayerUtils.sendActionBar(Component.empty().append(Component.text(stepDeltaString, NamedTextColor.GOLD, TextDecoration.BOLD))
                .append(Component.text(" [" + stepString + "]", NamedTextColor.YELLOW)));
    }

    private String formatDeltaTime(long deltaTime) {
        long seconds = deltaTime / 1000;
        long milliseconds = deltaTime % 1000;
        long minutes = seconds / 60;
        seconds = seconds % 60;
        return String.format("%02d:%02d.%03d", minutes, seconds, milliseconds);
    }

}
