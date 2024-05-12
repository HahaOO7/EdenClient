package at.haha007.edenclient.utils;

import at.haha007.edenclient.callbacks.JoinWorldCallback;
import at.haha007.edenclient.utils.tasks.CompleteCommandTask;
import at.haha007.edenclient.utils.tasks.Task;
import at.haha007.edenclient.utils.tasks.TaskManager;
import at.haha007.edenclient.utils.tasks.WaitForTicksTask;
import com.mojang.brigadier.tree.CommandNode;
import net.minecraft.client.Minecraft;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public enum PluginSignature {
    CHESTSHOP(Set.of("chestshop:iteminfo"), Map.of(), () -> false),
    CRAFTBOOK(Set.of("craftbook:sign"), Map.of(), () -> false),
    PWARP(Set.of("pwg", "pw", "pwarp", "pww"), Map.of(), () -> false),
    PLAYER_WARPS(Set.of("pwarp"), Map.of("pwarp am", "pwarp amount", "pwarp ab", "pwarp about"), PWARP::isPluginPresent),
    WORLDEDIT(Set.of("/", "/replace"), Map.of(), () -> false);

    static {
        JoinWorldCallback.EVENT.register(() -> Arrays.stream(values()).forEach(p -> p.lastCheck = 0), PluginSignature.class);
    }

    private long lastCheck = 0;
    private boolean matches = false;
    private final Set<String> commands;
    private final Map<String, String> tabCompletions;
    private final Supplier<Boolean> customDisable;

    PluginSignature(Set<String> commands, Map<String, String> tabCompletions, Supplier<Boolean> customDisable) {
        this.commands = commands;
        this.tabCompletions = tabCompletions;
        this.customDisable = customDisable;
    }

    public boolean isPluginPresent() {
        if (lastCheck < System.currentTimeMillis() - 1000 * 60) {
            update();
        }
        return matches;
    }

    private void update() {
        lastCheck = System.currentTimeMillis();
        Set<String> registered = Objects.requireNonNull(Minecraft.getInstance().getConnection()).getCommands().getRoot()
                .getChildren().stream().map(CommandNode::getName).collect(Collectors.toSet());
        if (!registered.containsAll(commands)) {
            matches = false;
            return;
        }
        if (tabCompletions.isEmpty()) {
            matches = true;
            return;
        }
        if(customDisable.get()) {
            matches = false;
            return;
        }
        TaskManager taskManager = new TaskManager();
        AtomicBoolean found = new AtomicBoolean(true);
        for (Map.Entry<String, String> entry : tabCompletions.entrySet()) {
            CompleteCommandTask completeCommandTask = new CompleteCommandTask(entry.getKey());
            Task task = completeCommandTask.then(() -> {
                if (completeCommandTask.getSuggestions().contains(entry.getValue())) return;
                found.set(false);
            });
            taskManager.then(task);
            taskManager.then(new WaitForTicksTask(1));
        }
        taskManager.then(() -> matches = found.get());
        taskManager.start();
    }


}
