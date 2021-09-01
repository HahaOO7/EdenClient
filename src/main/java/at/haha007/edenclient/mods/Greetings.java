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

import java.util.*;
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
    private Map<String, String> specificPlayerGreetMessages;
    private Map<String, String> specificPlayerWBMessages;
    private List<String> welcomeNewPlayerMessages;
    private List<String> welcomeBackPlayerMessages;
    private List<String> greetOldPlayerMessages;
    private final Random random = new Random();

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
        specificPlayerGreetMessages = new HashMap<>();
        specificPlayerWBMessages = new HashMap<>();
        if (tag.isEmpty()) {
            enabled = false;
            minDelay = 1200;
            wbDelay = 1200;
            welcomeNewPlayerMessages = List.of("Hey %player%");
            greetOldPlayerMessages = List.of("Hey %player%");
            welcomeBackPlayerMessages = List.of("Wb %player%");
            return;
        }
        enabled = tag.getBoolean("enabled");
        minDelay = tag.getInt("delay");
        wbDelay = Math.min(tag.getInt("wbDelay"), 20);
        welcomeNewPlayerMessages = tag.getList("welcomeNewPlayerMessageList", 10).stream().map(nbt -> (NbtCompound) nbt).map(nbtCompound1 -> nbtCompound1.getString("player")).collect(Collectors.toList());
        greetOldPlayerMessages = tag.getList("greetOldPlayerMessageList", 10).stream().map(nbt -> (NbtCompound) nbt).map(nbtCompound1 -> nbtCompound1.getString("player")).collect(Collectors.toList());
        welcomeBackPlayerMessages = tag.getList("welcomeBackPlayerMessageList", 10).stream().map(nbt -> (NbtCompound) nbt).map(nbtCompound1 -> nbtCompound1.getString("player")).collect(Collectors.toList());
        ignoredPlayers = tag.getList("ignoredPlayers", 10).stream().map(nbt -> (NbtCompound) nbt).map(nbtCompound1 -> nbtCompound1.getString("player")).collect(Collectors.toSet());
        tag.getList("specificGreetMessageList", 10).stream().map(nbt -> (NbtCompound) nbt).forEach(nbt -> specificPlayerGreetMessages.put(nbt.getString("player"), nbt.getString("message")));
        tag.getList("specificWBMessageList", 10).stream().map(nbt -> (NbtCompound) nbt).forEach(nbt -> specificPlayerWBMessages.put(nbt.getString("player"), nbt.getString("message")));
    }

    private void save(NbtCompound nbtCompound) {
        NbtCompound tag = new NbtCompound();
        tag.putBoolean("enabled", enabled);
        tag.putInt("delay", minDelay);
        tag.putInt("wbDelay", wbDelay);

        NbtList welcomeNewPlayerMessageList = new NbtList();
        welcomeNewPlayerMessages.forEach(s -> {
            NbtCompound messageComp = new NbtCompound();
            messageComp.putString("message", s);
            welcomeNewPlayerMessageList.add(messageComp);
        });
        tag.put("welcomeNewPlayerMessageList", welcomeNewPlayerMessageList);

        NbtList greetOldPlayerMessageList = new NbtList();
        greetOldPlayerMessages.forEach(s -> {
            NbtCompound messageComp = new NbtCompound();
            messageComp.putString("message", s);
            greetOldPlayerMessageList.add(messageComp);
        });
        tag.put("greetOldPlayerMessageList", greetOldPlayerMessageList);

        NbtList welcomeBackPlayerMessageList = new NbtList();
        welcomeBackPlayerMessages.forEach(s -> {
            NbtCompound messageComp = new NbtCompound();
            messageComp.putString("message", s);
            welcomeBackPlayerMessageList.add(messageComp);
        });
        tag.put("welcomeBackPlayerMessageList", welcomeBackPlayerMessageList);

        NbtList ignoredPlayersComp = new NbtList();
        ignoredPlayers.forEach(s -> {
            NbtCompound playerComp = new NbtCompound();
            playerComp.putString("player", s);
            ignoredPlayersComp.add(playerComp);
        });
        tag.put("ignoredPlayers", ignoredPlayersComp);

        NbtList specificGreetMessageList = new NbtList();
        specificPlayerGreetMessages.forEach((key, value) -> {
            NbtCompound greetComp = new NbtCompound();
            greetComp.putString("player", key);
            greetComp.putString("message", value);
            specificGreetMessageList.add(greetComp);
        });
        tag.put("specificGreetMessageList", specificGreetMessageList);

        NbtList specificWBMessageList = new NbtList();
        specificPlayerWBMessages.forEach((key, value) -> {
            NbtCompound wbComp = new NbtCompound();
            wbComp.putString("player", key);
            wbComp.putString("message", value);
            specificWBMessageList.add(wbComp);
        });
        tag.put("specificWBMessageList", specificWBMessageList);

        nbtCompound.put("greeting", tag);
    }

    private void onChat(AddChatMessageCallback.ChatAddEvent chatAddEvent) {
        if (!enabled) return;
        String msg = chatAddEvent.getChatText().getString();
        if (msg.length() < 5) return;

        if (msg.startsWith("[-] ")) {
            String name = msg.substring(4);
            quitPlayers.add(name);
            Scheduler.get().scheduleSyncDelayed(() -> quitPlayers.remove(name), wbDelay);
        }

        if (msg.startsWith("[+] ")) {
            String name = msg.substring(4);
            if (ignoredPlayers.contains(name.toLowerCase())) return;
            if (quitPlayers.contains(name)) {
                Collections.shuffle(welcomeBackPlayerMessages);
                addDelay(name, specificPlayerWBMessages.containsKey(name.toLowerCase()) ? specificPlayerWBMessages.get(name.toLowerCase()) : welcomeBackPlayerMessages.get(0));
            } else {
                Collections.shuffle(greetOldPlayerMessages);
                addDelay(name, specificPlayerGreetMessages.containsKey(name.toLowerCase()) ? specificPlayerGreetMessages.get(name.toLowerCase()) : greetOldPlayerMessages.get(0));
            }
        } else if (msg.substring(1).startsWith(" Willkommen, ") && msg.endsWith(" :) Viel Spaß auf OpenMC!")) {
            String name = msg.substring(14, msg.length() - 25);
            Collections.shuffle(welcomeNewPlayerMessages);
            addDelay(name, welcomeNewPlayerMessages.get(0));
        }
    }

    private void addDelay(String name, String message) {
        if (sentPlayers.contains(name)) return;
        sentPlayers.add(name);
        Scheduler.get().scheduleSyncDelayed(() -> {
            PlayerUtils.messageC2S(message.replace("%player%", name));
            Scheduler.get().scheduleSyncDelayed(() -> sentPlayers.remove(name), minDelay);
        }, random.nextInt(100) + 60);
    }

    private void registerCommand() {
        LiteralArgumentBuilder<ClientCommandSource> cmd = literal("greet");

        cmd.then(literal("new").then(literal("add").then(argument("text", StringArgumentType.greedyString()).executes(c -> {
            welcomeNewPlayerMessages.add(c.getArgument("text", String.class));
            PlayerUtils.sendModMessage("Message added");
            return 1;
        }))));

        cmd.then(literal("new").then(literal("remove").then(argument("text", StringArgumentType.greedyString()).suggests(this::suggestWelcomeNewPlayerMessages).executes(c -> {
            if (welcomeNewPlayerMessages.remove(c.getArgument("text", String.class))) {
                PlayerUtils.sendModMessage("Message removed");
            } else {
                PlayerUtils.sendModMessage("This message doesnt exist");
            }
            return 1;
        }))));

        cmd.then(literal("new").then(literal("list").executes(c -> {
            List<String> newMessages = welcomeNewPlayerMessages.stream().toList();
            if (newMessages.size() == 0){
                PlayerUtils.sendModMessage("No messages registered.");
            }
            PlayerUtils.sendModMessage("Messages for new players:");
            for (int i = 0; i < newMessages.size(); i++) {
                PlayerUtils.sendModMessage(new LiteralText("[" + (i + 1) + "] ").formatted(Formatting.GOLD)
                        .append(new LiteralText(newMessages.get(i)).formatted(Formatting.AQUA)));
            }
            return 1;
        })));

        cmd.then(literal("old").then(literal("add").then(argument("text", StringArgumentType.greedyString()).executes(c -> {
            greetOldPlayerMessages.add(c.getArgument("text", String.class));
            PlayerUtils.sendModMessage("Message added");
            return 1;
        }))));

        cmd.then(literal("old").then(literal("remove").then(argument("text", StringArgumentType.greedyString()).suggests(this::suggestWelcomeOldPlayerMessages).executes(c -> {
            if (greetOldPlayerMessages.remove(c.getArgument("text", String.class))) {
                PlayerUtils.sendModMessage("Message removed");
            } else {
                PlayerUtils.sendModMessage("This message doesnt exist");
            }
            return 1;
        }))));

        cmd.then(literal("old").then(literal("list").executes(c -> {
            List<String> oldMessages = greetOldPlayerMessages.stream().toList();
            if (oldMessages.size() == 0){
                PlayerUtils.sendModMessage("No messages registered.");
            }
            PlayerUtils.sendModMessage("Messages for old players:");
            for (int i = 0; i < oldMessages.size(); i++) {
                PlayerUtils.sendModMessage(new LiteralText("[" + (i + 1) + "] ").formatted(Formatting.GOLD)
                        .append(new LiteralText(oldMessages.get(i)).formatted(Formatting.AQUA)));
            }
            return 1;
        })));

        cmd.then(literal("wb").then(literal("add").then(argument("text", StringArgumentType.greedyString()).executes(c -> {
            welcomeBackPlayerMessages.add(c.getArgument("text", String.class));
            PlayerUtils.sendModMessage("Message added");
            return 1;
        }))));

        cmd.then(literal("wb").then(literal("remove").then(argument("text", StringArgumentType.greedyString()).suggests(this::suggestWelcomeBackPlayerMessages).executes(c -> {
            if (welcomeBackPlayerMessages.remove(c.getArgument("text", String.class))) {
                PlayerUtils.sendModMessage("Message removed");
            } else {
                PlayerUtils.sendModMessage("This message doesnt exist");
            }
            return 1;
        }))));

        cmd.then(literal("wb").then(literal("list").executes(c -> {
            List<String> wbMessages = welcomeBackPlayerMessages.stream().toList();
            if (wbMessages.size() == 0){
                PlayerUtils.sendModMessage("No messages registered.");
            }
            PlayerUtils.sendModMessage("Messages for welcome-back players:");
            for (int i = 0; i < wbMessages.size(); i++) {
                PlayerUtils.sendModMessage(new LiteralText("[" + (i + 1) + "] ").formatted(Formatting.GOLD)
                        .append(new LiteralText(wbMessages.get(i)).formatted(Formatting.AQUA)));
            }
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

        cmd.then(literal("player").then(literal("greet").then(literal("add").then(argument("player", StringArgumentType.word()).then(argument("phrase", StringArgumentType.greedyString()).executes(c -> {
            String player = c.getArgument("player", String.class);
            if (!player.matches("[A-Za-z0-9_]{4,16}")) {
                PlayerUtils.sendModMessage("Your input is not a valid username.");
                return 0;
            }
            if (specificPlayerGreetMessages.containsKey(player.toLowerCase())) {
                PlayerUtils.sendModMessage("Already contained an greet-entry for this player. Updated entry.");
            } else {
                PlayerUtils.sendModMessage("Added new greet-entry for player.");
            }
            specificPlayerGreetMessages.put(player.toLowerCase(), c.getArgument("phrase", String.class));

            return 1;
        }))))));

        cmd.then(literal("player").then(literal("greet").then(literal("remove").then(argument("player", StringArgumentType.word()).suggests(this::suggestSpecificGreetMessagePlayers).executes(c -> {
            String player = c.getArgument("player", String.class);

            if (specificPlayerGreetMessages.remove(player.toLowerCase()) != null) {
                PlayerUtils.sendModMessage(new LiteralText("Removed greet message for ").formatted(Formatting.GOLD)
                        .append(new LiteralText(player)).formatted(Formatting.AQUA));
            } else {
                PlayerUtils.sendModMessage("No message found for that player.");
            }

            return 1;
        })))));

        cmd.then(literal("player").then(literal("greet").then(literal("list").executes(c -> {
            if (specificPlayerGreetMessages.entrySet().size() == 0) {
                PlayerUtils.sendModMessage("No specific messages for any player registered.");
                return 0;
            }

            List<Map.Entry<String, String>> list = specificPlayerGreetMessages.entrySet().stream().toList();
            for (int i = 0; i < list.size(); i++) {
                PlayerUtils.sendModMessage(new LiteralText("[" + (i + 1) + "] ").formatted(Formatting.GOLD)
                        .append(new LiteralText(String.format("%-17s - %s", list.get(i).getKey(), list.get(i).getValue())).formatted(Formatting.AQUA)));
            }
            return 1;
        }))));

        cmd.then(literal("player").then(literal("wb").then(literal("add").then(argument("player", StringArgumentType.word()).then(argument("phrase", StringArgumentType.greedyString()).executes(c -> {
            String player = c.getArgument("player", String.class);
            if (!player.matches("[A-Za-z0-9_]{4,16}")) {
                PlayerUtils.sendModMessage("Your input is not a valid username.");
                return 0;
            }
            if (specificPlayerWBMessages.containsKey(player.toLowerCase())) {
                PlayerUtils.sendModMessage("Already contained a wb-entry for this player. Updated entry.");
            } else {
                PlayerUtils.sendModMessage("Added new wb-entry for player.");
            }
            specificPlayerWBMessages.put(player.toLowerCase(), c.getArgument("phrase", String.class));

            return 1;
        }))))));

        cmd.then(literal("player").then(literal("wb").then(literal("remove").then(argument("player", StringArgumentType.word()).suggests(this::suggestSpecificWBMessagePlayers).executes(c -> {
            String player = c.getArgument("player", String.class);

            if (specificPlayerWBMessages.remove(player.toLowerCase()) != null) {
                PlayerUtils.sendModMessage(new LiteralText("Removed wb message for ").formatted(Formatting.GOLD)
                        .append(new LiteralText(player)).formatted(Formatting.AQUA));
            } else {
                PlayerUtils.sendModMessage("No wb-message found for that player.");
            }

            return 1;
        })))));

        cmd.then(literal("player").then(literal("wb").then(literal("list").executes(c -> {
            if (specificPlayerWBMessages.entrySet().size() == 0) {
                PlayerUtils.sendModMessage("No specific messages for any player registered.");
                return 0;
            }

            List<Map.Entry<String, String>> list = specificPlayerWBMessages.entrySet().stream().toList();
            for (int i = 0; i < list.size(); i++) {
                PlayerUtils.sendModMessage(new LiteralText("[" + (i + 1) + "] ").formatted(Formatting.GOLD)
                        .append(new LiteralText(String.format("%-17s - %s", list.get(i).getKey(), list.get(i).getValue())).formatted(Formatting.AQUA)));
            }
            return 1;
        }))));

        register(cmd);
    }

    private CompletableFuture<Suggestions> suggestWelcomeBackPlayerMessages
            (CommandContext<ClientCommandSource> clientCommandSourceCommandContext, SuggestionsBuilder suggestionsBuilder) {
        welcomeBackPlayerMessages.forEach(suggestionsBuilder::suggest);
        return suggestionsBuilder.buildFuture();
    }

    private CompletableFuture<Suggestions> suggestWelcomeOldPlayerMessages
            (CommandContext<ClientCommandSource> clientCommandSourceCommandContext, SuggestionsBuilder suggestionsBuilder) {
        greetOldPlayerMessages.forEach(suggestionsBuilder::suggest);
        return suggestionsBuilder.buildFuture();
    }

    private CompletableFuture<Suggestions> suggestWelcomeNewPlayerMessages
            (CommandContext<ClientCommandSource> clientCommandSourceCommandContext, SuggestionsBuilder suggestionsBuilder) {
        welcomeNewPlayerMessages.forEach(suggestionsBuilder::suggest);
        return suggestionsBuilder.buildFuture();
    }

    private CompletableFuture<Suggestions> suggestIgnoredPlayers
            (CommandContext<ClientCommandSource> clientCommandSourceCommandContext, SuggestionsBuilder suggestionsBuilder) {
        ignoredPlayers.forEach(suggestionsBuilder::suggest);
        return suggestionsBuilder.buildFuture();
    }

    private CompletableFuture<Suggestions> suggestSpecificGreetMessagePlayers
            (CommandContext<ClientCommandSource> clientCommandSourceCommandContext, SuggestionsBuilder suggestionsBuilder) {
        specificPlayerGreetMessages.keySet().forEach(suggestionsBuilder::suggest);
        return suggestionsBuilder.buildFuture();
    }

    private CompletableFuture<Suggestions> suggestSpecificWBMessagePlayers
            (CommandContext<ClientCommandSource> clientCommandSourceCommandContext, SuggestionsBuilder suggestionsBuilder) {
        specificPlayerWBMessages.keySet().forEach(suggestionsBuilder::suggest);
        return suggestionsBuilder.buildFuture();
    }

}
