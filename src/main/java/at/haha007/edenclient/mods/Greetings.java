package at.haha007.edenclient.mods;

import at.haha007.edenclient.callbacks.AddChatMessageCallback;
import at.haha007.edenclient.callbacks.ConfigLoadCallback;
import at.haha007.edenclient.callbacks.ConfigSaveCallback;
import at.haha007.edenclient.utils.PlayerUtils;
import at.haha007.edenclient.utils.Scheduler;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.client.network.ClientCommandSource;
import net.minecraft.nbt.NbtCompound;

import java.util.HashSet;
import java.util.Set;

import static at.haha007.edenclient.command.CommandManager.*;

public class Greetings {
    private boolean enabled;
    private int minDelay;
    private Set<String> sentPlayers;
    private String newPlayer;
    private String oldPlayer;

    public Greetings() {
        registerCommand();
        AddChatMessageCallback.EVENT.register(this::onChat);
        ConfigLoadCallback.EVENT.register(this::load);
        ConfigSaveCallback.EVENT.register(this::save);
    }

    private void load(NbtCompound nbtCompound) {
        NbtCompound tag = nbtCompound.getCompound("greeting");
        sentPlayers = new HashSet<>();
        if (tag.isEmpty()) {
            enabled = false;
            minDelay = 1200;
            newPlayer = "Hey %player%";
            oldPlayer = "Hey %player%";
            return;
        }
        enabled = tag.getBoolean("enabled");
        minDelay = tag.getInt("delay");
        newPlayer = tag.getString("newPlayer");
        oldPlayer = tag.getString("oldPlayer");
    }

    private void save(NbtCompound nbtCompound) {
        NbtCompound tag = new NbtCompound();
        tag.putBoolean("enabled", enabled);
        tag.putInt("delay", minDelay);
        tag.putString("newPlayer", newPlayer);
        tag.putString("oldPlayer", oldPlayer);

        nbtCompound.put("greeting", tag);
    }

    private void onChat(AddChatMessageCallback.ChatAddEvent chatAddEvent) {
        if (!enabled) return;
        String msg = chatAddEvent.getChatText().getString();
        if (msg.length() < 5) return;
        if (msg.startsWith("[+] ")) {
            String name = msg.substring(4);
            addDelay(name, oldPlayer);
        } else if (msg.substring(1).startsWith(" Willkommen, ") && msg.endsWith(" :) Viel spaß auf openMC!")) {
            String name = msg.substring(14, msg.length() - 25);
            addDelay(name, newPlayer);
        }
    }

    private void addDelay(String name, String message) {
        if (sentPlayers.contains(name)) return;
        sentPlayers.add(name);
        System.out.println(name);
        Scheduler.get().scheduleSyncDelayed(() -> {
            PlayerUtils.messageC2S(message.replace("%player%", name));
            Scheduler.get().scheduleSyncDelayed(() -> sentPlayers.remove(name), minDelay);
        }, 60);
    }

    private void registerCommand() {
        LiteralArgumentBuilder<ClientCommandSource> cmd = literal("greet");
        cmd.then(literal("new").then(argument("text", StringArgumentType.greedyString()).executes(c -> {
            newPlayer = c.getArgument("text", String.class);
            PlayerUtils.sendModMessage("Message updated");
            return 1;
        })));
        cmd.then(literal("old").then(argument("text", StringArgumentType.greedyString()).executes(c -> {
            oldPlayer = c.getArgument("text", String.class);
            PlayerUtils.sendModMessage("Message updated");
            return 1;
        })));
        cmd.then(literal("delay").then(argument("delay", IntegerArgumentType.integer(1)).executes(c -> {
            minDelay = c.getArgument("delay", Integer.class) * 20;
            PlayerUtils.sendModMessage("Minimal delay updated");
            return 1;
        })));
        cmd.then(literal("toggle").executes(c -> {
            enabled = !enabled;
            PlayerUtils.sendModMessage(enabled ? "Greetings enabled" : "Greetings disabled");
            return 1;
        }));
        register(cmd);
    }
}
