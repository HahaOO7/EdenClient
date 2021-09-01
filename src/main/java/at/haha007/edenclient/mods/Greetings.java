package at.haha007.edenclient.mods;

import at.haha007.edenclient.callbacks.AddChatMessageCallback;
import at.haha007.edenclient.callbacks.ConfigLoadCallback;
import at.haha007.edenclient.callbacks.ConfigSaveCallback;
import at.haha007.edenclient.utils.PlayerUtils;
import at.haha007.edenclient.utils.Scheduler;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.client.network.ClientCommandSource;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Formatting;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static at.haha007.edenclient.command.CommandManager.*;

public class Greetings {
    private boolean enabled;
    private int minDelay;
    private int wbDelay;
    private Set<String> sentPlayers;
    private Set<String> quitPlayers;
    private Set<String> ignoredPlayers;
    private String newPlayer;
    private String wbText;
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
        quitPlayers = new HashSet<>();
        ignoredPlayers = new HashSet<>();
        if (tag.isEmpty()) {
            enabled = false;
            minDelay = 1200;
            wbDelay = 1200;
            newPlayer = "Hey %player%";
            oldPlayer = "Hey %player%";
            wbText = "Wb %player%";
            return;
        }
        enabled = tag.getBoolean("enabled");
        minDelay = tag.getInt("delay");
        wbDelay = Math.min(tag.getInt("wbDelay"), 20);
        newPlayer = tag.getString("newPlayer");
        oldPlayer = tag.getString("oldPlayer");
        wbText = tag.getString("wbText");
        ignoredPlayers = tag.getList("ignoredPlayers", 10).stream().map(nbt -> (NbtCompound) nbt).map(nbtCompound1 -> nbtCompound1.getString("player")).collect(Collectors.toSet());
    }

    private void save(NbtCompound nbtCompound) {
        NbtCompound tag = new NbtCompound();
        tag.putBoolean("enabled", enabled);
        tag.putInt("delay", minDelay);
        tag.putInt("wbDelay", wbDelay);
        tag.putString("newPlayer", newPlayer);
        tag.putString("oldPlayer", oldPlayer);
        tag.putString("wbText", wbText);
        NbtList ignoredPlayersComp = new NbtList();
        ignoredPlayers.forEach(s -> {
            NbtCompound playerComp = new NbtCompound();
            playerComp.putString("player", s);
            ignoredPlayersComp.add(playerComp);
        });
        tag.put("ignoredPlayers", ignoredPlayersComp);
        nbtCompound.put("greeting", tag);
    }

    private void onChat(AddChatMessageCallback.ChatAddEvent chatAddEvent) {
        if (!enabled) return;
        String msg = chatAddEvent.getChatText().getString();
        if (msg.length() < 5) return;

        if (msg.startsWith("[-] ")) {
            String name = msg.substring(4);
            if (ignoredPlayers.contains(name.toLowerCase())) return;
            quitPlayers.add(name);
            Scheduler.get().scheduleSyncDelayed(() -> quitPlayers.remove(name), wbDelay);
        }

        if (msg.startsWith("[+] ")) {
            String name = msg.substring(4);
            if (quitPlayers.contains(name))
                addDelay(name, wbText);
            else
                addDelay(name, oldPlayer);
        } else if (msg.substring(1).startsWith(" Willkommen, ") && msg.endsWith(" :) Viel Spaß auf OpenMC!")) {
            String name = msg.substring(14, msg.length() - 25);
            addDelay(name, newPlayer);
        }
    }

    private void addDelay(String name, String message) {
        if (sentPlayers.contains(name)) return;
        sentPlayers.add(name);
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

        cmd.then(literal("wb").then(argument("text", StringArgumentType.greedyString()).executes(c -> {
            wbText = c.getArgument("text", String.class);
            PlayerUtils.sendModMessage("Message updated");
            return 1;
        })));

        cmd.then(literal("delay").then(argument("delay", IntegerArgumentType.integer(1)).executes(c -> {
            minDelay = c.getArgument("delay", Integer.class) * 20;
            PlayerUtils.sendModMessage("Minimal delay updated");
            return 1;
        })));

        cmd.then(literal("wbdelay").then(argument("delay", IntegerArgumentType.integer(1)).executes(c -> {
            wbDelay = c.getArgument("delay", Integer.class) * 20;
            PlayerUtils.sendModMessage("Wb delay updated");
            return 1;
        })));

        cmd.then(literal("toggle").executes(c -> {
            enabled = !enabled;
            PlayerUtils.sendModMessage(enabled ? "Greetings enabled" : "Greetings disabled");
            return 1;
        }));

        cmd.then(literal("ignore").then(literal("add").then(argument("player", StringArgumentType.word()).executes(c -> {
            String player = c.getArgument("player", String.class);
            if (!player.matches("[A-Za-z0-9_]{4,16}")) {
                PlayerUtils.sendModMessage("Your input is not a valid username.");
                return 0;
            }
            ignoredPlayers.add(player.toLowerCase());
            PlayerUtils.sendModMessage(new LiteralText("Added ").formatted(Formatting.GOLD)
                    .append(new LiteralText(player).formatted(Formatting.AQUA))
                    .append(new LiteralText(" to ignored players.").formatted(Formatting.GOLD)));
            return 1;
        }))));

        cmd.then(literal("ignore").then(literal("remove").then(argument("player", StringArgumentType.word()).suggests(this::suggestIgnoredPlayers).executes(c -> {
            String player = c.getArgument("player", String.class);
            if (ignoredPlayers.remove(player.toLowerCase())) {
                PlayerUtils.sendModMessage(new LiteralText("Successfully removed ").formatted(Formatting.GOLD)
                        .append(new LiteralText(player).formatted(Formatting.AQUA))
                        .append(" from your ignored players.").formatted(Formatting.GOLD));
                return 1;
            } else {
                PlayerUtils.sendModMessage("This player is not ignored and can therefore not be removed.");
                return 0;
            }
        }))));

        cmd.then(literal("ignore").then(literal("list").executes(c -> {
            if (ignoredPlayers.size() == 0) {
                PlayerUtils.sendModMessage("No players ignored for Greetings.");
                return 0;
            }

            List<String> ignoredPlayersList = ignoredPlayers.stream().sorted().collect(Collectors.toList());
            PlayerUtils.sendModMessage("Ignored players for Greetings:");
            for (int i = 0; i < ignoredPlayers.size(); i++) {
                PlayerUtils.sendModMessage(new LiteralText("[" + (i + 1) + "] ").formatted(Formatting.GOLD)
                        .append(new LiteralText(ignoredPlayersList.get(i)).formatted(Formatting.AQUA)));
            }
            return 1;
        })));

        register(cmd);
    }

    private CompletableFuture<Suggestions> suggestIgnoredPlayers(CommandContext<ClientCommandSource> clientCommandSourceCommandContext, SuggestionsBuilder suggestionsBuilder) {
        ignoredPlayers.forEach(suggestionsBuilder::suggest);
        return suggestionsBuilder.buildFuture();
    }
}
