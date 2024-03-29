package at.haha007.edenclient.mods;

import at.haha007.edenclient.EdenClient;
import at.haha007.edenclient.annotations.Mod;
import at.haha007.edenclient.callbacks.AddChatMessageCallback;
import at.haha007.edenclient.utils.ChatColor;
import at.haha007.edenclient.utils.PlayerUtils;
import at.haha007.edenclient.utils.Scheduler;
import at.haha007.edenclient.utils.config.ConfigSubscriber;
import at.haha007.edenclient.utils.config.PerWorldConfig;
import at.haha007.edenclient.utils.config.wrappers.StringList;
import at.haha007.edenclient.utils.config.wrappers.StringSet;
import at.haha007.edenclient.utils.config.wrappers.StringStringMap;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;

import java.util.*;
import java.util.concurrent.CompletableFuture;

import static at.haha007.edenclient.command.CommandManager.*;

@Mod
public class Greetings {
    @ConfigSubscriber("false")
    private boolean enabled;
    @ConfigSubscriber("1200")
    private int minDelay;
    @ConfigSubscriber("1200")
    private int wbMinDelay;
    @ConfigSubscriber("1200")
    private int wbMaxDelay;
    @ConfigSubscriber("Haha007")
    private StringSet ignoredPlayers;
    @ConfigSubscriber
    private StringStringMap specificPlayerGreetMessages;
    @ConfigSubscriber
    private StringStringMap specificPlayerWBMessages;
    @ConfigSubscriber("Hey %player%")
    private StringList welcomeNewPlayerMessages;
    @ConfigSubscriber("Wb %player%")
    private StringList welcomeBackPlayerMessages;
    @ConfigSubscriber("Huhu %player%")
    private StringList greetOldPlayerMessages;

    private final Set<String> sentPlayers = new HashSet<>();
    private final Map<String, Long> quitTimes = new HashMap<>();
    private final Random random = new Random();

    public Greetings() {
        registerCommand();
        AddChatMessageCallback.EVENT.register(this::onChat);
        PerWorldConfig.get().register(this, "greeting");
    }

    private void onChat(AddChatMessageCallback.ChatAddEvent chatAddEvent) {
        if (!enabled) return;
        String msg = chatAddEvent.getChatText().getString();
        if (msg.length() < 5) return;

        if (msg.startsWith("[-] ")) {
            String name = msg.substring(4);
            quitTimes.put(name, System.currentTimeMillis());
            EdenClient.getMod(Scheduler.class).scheduleSyncDelayed(() -> quitTimes.remove(name), wbMaxDelay);
        }

        if (msg.startsWith("[+] ")) {
            String name = msg.substring(4);
            if (ignoredPlayers.contains(name.toLowerCase())) return;
            if (quitTimes.containsKey(name)) {
                if (quitTimes.get(name) + wbMinDelay < System.currentTimeMillis()) return;
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
        EdenClient.getMod(Scheduler.class).scheduleSyncDelayed(() -> {
            PlayerUtils.messageC2S(message.replace("%player%", name));
            EdenClient.getMod(Scheduler.class).scheduleSyncDelayed(() -> sentPlayers.remove(name), minDelay);
        }, random.nextInt(100) + 60);
    }

    private void registerCommand() {
        LiteralArgumentBuilder<ClientSuggestionProvider> cmd = literal("egreet");

        /*
        MESSAGES FOR NEW PLAYERS
         */
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
            if (newMessages.isEmpty()) {
                PlayerUtils.sendModMessage("No messages registered.");
            }
            PlayerUtils.sendModMessage("Messages for new players:");
            for (int i = 0; i < newMessages.size(); i++) {
                PlayerUtils.sendModMessage(ChatColor.GOLD + "[" + (i + 1) + "] " + ChatColor.AQUA + newMessages.get(i));
            }
            return 1;
        })));

        /*
        MESSAGES FOR OLD PLAYERS
        */

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
            if (oldMessages.isEmpty()) {
                PlayerUtils.sendModMessage("No messages registered.");
            }
            PlayerUtils.sendModMessage("Messages for old players:");
            for (int i = 0; i < oldMessages.size(); i++) {
                PlayerUtils.sendModMessage(ChatColor.GOLD + "[" + (i + 1) + "] " + ChatColor.AQUA + oldMessages.get(i));
            }
            return 1;
        })));

        /*
        MESSAGES FOR PLAYERS WHO ALREADY JOINED THIS SESSION ONCE
        */

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
            if (wbMessages.isEmpty()) {
                PlayerUtils.sendModMessage("No messages registered.");
            }
            PlayerUtils.sendModMessage("Messages for welcome-back players:");
            for (int i = 0; i < wbMessages.size(); i++) {
                PlayerUtils.sendModMessage(ChatColor.GOLD + "[" + (i + 1) + "] " + ChatColor.AQUA + wbMessages.get(i));
            }
            return 1;
        })));

        /*
        SET MINIMUM DELAY IN SECONDS UNTIL A PLAYERS GETS GREETED TWICE
        */

        cmd.then(literal("delay").then(argument("delay", IntegerArgumentType.integer(1)).executes(c -> {
            minDelay = c.getArgument("delay", Integer.class) * 20;
            PlayerUtils.sendModMessage("Minimal delay updated");
            return 1;
        })));

        /*
        SET MINIMUM DELAY IN SECONDS UNTIL A PLAYERS GETS GREETED TWICE AFTER RETURNING
        */

        cmd.then(literal("wbdelay").then(argument("minDelay", IntegerArgumentType.integer(1))
                .then(argument("maxDelay", IntegerArgumentType.integer(1)).executes(c -> {
                    wbMinDelay = c.getArgument("minDelay", Integer.class) * 20;
                    wbMaxDelay = c.getArgument("minDelay", Integer.class) * 20;
                    PlayerUtils.sendModMessage("Wb delay updated");
                    return 1;
                }))));

        /*
        TOGGLE IF GREETINGS ARE SENT
        */

        cmd.then(literal("toggle").executes(c -> {
            enabled = !enabled;
            PlayerUtils.sendModMessage(enabled ? "Greetings enabled" : "Greetings disabled");
            return 1;
        }));

        /*
        IGNORE PLAYERS SO NO GREETINGS ARE SENT TO THEM
        */

        cmd.then(literal("ignore").then(literal("add").then(argument("player", StringArgumentType.word()).executes(c -> {
            String player = c.getArgument("player", String.class);
            if (!player.matches("[A-Za-z0-9_]{4,16}")) {
                PlayerUtils.sendModMessage("Your input is not a valid username.");
                return 0;
            }
            ignoredPlayers.add(player.toLowerCase());
            PlayerUtils.sendModMessage(ChatColor.GOLD + "Added " + ChatColor.AQUA + player + ChatColor.GOLD + " to ignored players.");
            return 1;
        }))));

        cmd.then(literal("ignore").then(literal("remove").then(argument("player", StringArgumentType.word()).suggests(this::suggestIgnoredPlayers).executes(c -> {
            String player = c.getArgument("player", String.class);
            if (ignoredPlayers.remove(player.toLowerCase())) {
                PlayerUtils.sendModMessage(ChatColor.GOLD + "Removed " + ChatColor.AQUA + player + ChatColor.GOLD + " from ignored players.");
                return 1;
            } else {
                PlayerUtils.sendModMessage(ChatColor.GOLD + "This player is not ignored and can therefore not be removed.");
                return 0;
            }
        }))));

        cmd.then(literal("ignore").then(literal("list").executes(c -> {
            if (ignoredPlayers.isEmpty()) {
                PlayerUtils.sendModMessage(ChatColor.GOLD + "No players ignored for Greetings.");
                return 0;
            }

            List<String> ignoredPlayersList = ignoredPlayers.stream().sorted().toList();
            PlayerUtils.sendModMessage(ChatColor.GOLD + "Ignored players for Greetings:");
            for (int i = 0; i < ignoredPlayers.size(); i++) {
                PlayerUtils.sendModMessage(ChatColor.GOLD + "[" + (i + 1) + "] " + ChatColor.AQUA + ignoredPlayersList.get(i));
            }
            return 1;
        })));

        /*
        GREET MESSAGES FOR SPECIFIC PLAYERS
        */

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
                PlayerUtils.sendModMessage(ChatColor.GOLD + "Removed greet message for " + ChatColor.AQUA + player);
            } else {
                PlayerUtils.sendModMessage(ChatColor.GOLD + "No message found for that player.");
            }

            return 1;
        })))));

        cmd.then(literal("player").then(literal("greet").then(literal("list").executes(c -> {
            if (specificPlayerGreetMessages.entrySet().isEmpty()) {
                PlayerUtils.sendModMessage(ChatColor.GOLD + "No specific messages for any player registered.");
                return 0;
            }

            List<Map.Entry<String, String>> list = specificPlayerGreetMessages.entrySet().stream().toList();
            for (int i = 0; i < list.size(); i++) {
                PlayerUtils.sendModMessage(ChatColor.GOLD + "[" + (i + 1) + "] " + ChatColor.AQUA + String.format("%s - %s", list.get(i).getKey(), list.get(i).getValue()));
            }
            return 1;
        }))));

        /*
        WELCOME BACK MESSAGES FOR SPECIFIC PLAYERS
        */

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
                PlayerUtils.sendModMessage(ChatColor.GOLD + "Removed wb message for " + ChatColor.AQUA + player);
            } else {
                PlayerUtils.sendModMessage("No wb-message found for that player.");
            }

            return 1;
        })))));

        cmd.then(literal("player").then(literal("wb").then(literal("list").executes(c -> {
            if (specificPlayerWBMessages.entrySet().isEmpty()) {
                PlayerUtils.sendModMessage("No specific messages for any player registered.");
                return 0;
            }

            List<Map.Entry<String, String>> list = specificPlayerWBMessages.entrySet().stream().toList();
            for (int i = 0; i < list.size(); i++) {
                PlayerUtils.sendModMessage(ChatColor.GOLD + "[" + (i + 1) + "] " + ChatColor.AQUA + String.format("%s - %s", list.get(i).getKey(), list.get(i).getValue()));
            }
            return 1;
        }))));

        register(cmd,
               "Greetings allows you to automatically greet all players joining the server.",
               "You can also set specific messages or each player as well as different messages for new players or players that have previously joined in this session already.");
    }

    private CompletableFuture<Suggestions> suggestWelcomeBackPlayerMessages
            (CommandContext<ClientSuggestionProvider> clientCommandSourceCommandContext, SuggestionsBuilder suggestionsBuilder) {
        welcomeBackPlayerMessages.forEach(suggestionsBuilder::suggest);
        return suggestionsBuilder.buildFuture();
    }

    private CompletableFuture<Suggestions> suggestWelcomeOldPlayerMessages
            (CommandContext<ClientSuggestionProvider> clientCommandSourceCommandContext, SuggestionsBuilder suggestionsBuilder) {
        greetOldPlayerMessages.forEach(suggestionsBuilder::suggest);
        return suggestionsBuilder.buildFuture();
    }

    private CompletableFuture<Suggestions> suggestWelcomeNewPlayerMessages
            (CommandContext<ClientSuggestionProvider> clientCommandSourceCommandContext, SuggestionsBuilder suggestionsBuilder) {
        welcomeNewPlayerMessages.forEach(suggestionsBuilder::suggest);
        return suggestionsBuilder.buildFuture();
    }

    private CompletableFuture<Suggestions> suggestIgnoredPlayers
            (CommandContext<ClientSuggestionProvider> clientCommandSourceCommandContext, SuggestionsBuilder suggestionsBuilder) {
        ignoredPlayers.forEach(suggestionsBuilder::suggest);
        return suggestionsBuilder.buildFuture();
    }

    private CompletableFuture<Suggestions> suggestSpecificGreetMessagePlayers
            (CommandContext<ClientSuggestionProvider> clientCommandSourceCommandContext, SuggestionsBuilder suggestionsBuilder) {
        specificPlayerGreetMessages.keySet().forEach(suggestionsBuilder::suggest);
        return suggestionsBuilder.buildFuture();
    }

    private CompletableFuture<Suggestions> suggestSpecificWBMessagePlayers
            (CommandContext<ClientSuggestionProvider> clientCommandSourceCommandContext, SuggestionsBuilder suggestionsBuilder) {
        specificPlayerWBMessages.keySet().forEach(suggestionsBuilder::suggest);
        return suggestionsBuilder.buildFuture();
    }

}