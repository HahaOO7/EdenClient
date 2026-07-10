package at.haha007.edenclient.utils;

import at.haha007.edenclient.callbacks.JoinWorldCallback;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;

import java.util.Arrays;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public enum PluginSignature {
    CHESTSHOP(Set.of("chestshop:iteminfo"), () -> false),
    CRAFTBOOK(Set.of("craftbook:sign"), () -> false),
    PWARP(Set.of("pwg", "pw", "pwarp", "pww"), () -> false),
    PLAYER_WARPS(Set.of("pwarp"), PWARP::isPluginPresent),
    WORLDEDIT(Set.of("/", "/replace"), () -> false);

    static {
        JoinWorldCallback.EVENT.register(() -> Arrays.stream(values()).forEach(p -> p.lastCheck = 0), PluginSignature.class);
    }

    private long lastCheck = 0;
    private boolean matches = false;
    private final Set<String> commands;
    private final Supplier<Boolean> customDisable;

    PluginSignature(Set<String> commands, Supplier<Boolean> customDisable) {
        this.commands = commands;
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
        matches = registered.containsAll(commands) && !Boolean.TRUE.equals(customDisable.get());
        LogUtils.getLogger().info("Plugin signature for {} is {}", this, matches);
    }


}
