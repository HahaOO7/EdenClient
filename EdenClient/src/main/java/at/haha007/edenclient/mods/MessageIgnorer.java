package at.haha007.edenclient.mods;

import at.haha007.edenclient.annotations.Mod;
import at.haha007.edenclient.callbacks.AddChatMessageCallback;
import at.haha007.edenclient.utils.PlayerUtils;
import at.haha007.edenclient.utils.config.ConfigSubscriber;
import at.haha007.edenclient.utils.config.PerWorldConfig;
import at.haha007.edenclient.utils.config.wrappers.StringList;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.logging.LogUtils;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import static at.haha007.edenclient.command.CommandManager.*;
import static at.haha007.edenclient.utils.PlayerUtils.sendModMessage;

@Mod
public class MessageIgnorer {
    @ConfigSubscriber
    private final StringList regex = new StringList();
    @Setter
    @Getter
    @ConfigSubscriber("false")
    private boolean enabled;

    @Getter
    public enum Predefined {
        SELL("sellmessages",
                "Message ignoring for Sell Messages",
                "Verkauft für \\$(?<money>[0-9]{1,5}\\.?[0-9]{0,2}) \\((?<amount>[0-9,]{1,5}) (?<item>[a-zA-Z0-9_]{1,30}) Einheiten je \\$[0-9]{1,5}\\.?[0-9]{0,2}\\)",
                "\\$[0-9]{1,5}\\.?[0-9]{0,2} wurden deinem Konto hinzugefügt\\.",
                "Fehler: Du hast keine Berechtigung, diese benannten Gegenstände zu verkaufen: .*"),
        VOTE("votemessages",
                "Message ignoring for Vote Messages",
                ". \\/vote . [A-Za-z0-9_]{1,16} hat [0-9]{2}min Flugzeit erhalten\\.",
                ". \\/vote . [A-Za-z0-9_]{1,16} hat [1-4]{1} VoteC.ins? erhalten\\."),
        CHAT("globalchat",
                "Message ignoring for Global Chat Messages",
                "\\w+ \\| ~?\\w+ > .*"),
        DISCORD("discordchat",
                "Message ignoring for Discord Chat Messages",
                "[DC] [A-Za-z0-9]{1,16} > .*"),
        ITEM_INFO("iteminfo",
                "Message ignoring for ItemInfo messages",
                "Item Information: ?",
                "Voller Name: (?<originalname>[A-Za-z0-9_ ]{1,40})",
                "Shop Schild: (?<shortenedname>[A-Za-z0-9_ ]{1,40})",
                "\\/iteminfo \\(what's the item in hand\\?\\) ?",
                "\\/iteminfo log \\(what's the item ID of LOG\\?\\) ?"),
        WORLDEDIT("worldedit",
                "Message ignoring for WorldEdit EdenClient messages",
                "[0-9]{1,10} blocks have been replaced\\.");

        private final String[] regexes;
        private final String key;
        private final String message;

        Predefined(String key, String message, String... regexes) {
            this.key = key;
            this.regexes = regexes;
            this.message = message;
        }

    }

    public MessageIgnorer() {
        AddChatMessageCallback.EVENT.register(this::onChat, getClass());
        registerCommand("eignoremessage");
        registerCommand("eim");
        PerWorldConfig.get().register(this, "MessageIgnorer");
    }

    private void registerCommand(String cmd) {
        LiteralArgumentBuilder<ClientSuggestionProvider> node = literal(cmd);

        node.then(literal("toggle").executes(c -> {
            enabled = !enabled;
            String msg = enabled ? "Message ignoring enabled" : "Message ignoring disabled";
            sendModMessage(msg);
            return 1;
        }));

        node.then(literal("add").then(argument("regex", StringArgumentType.greedyString()).executes(c -> {
            String im = c.getArgument("regex", String.class);
            if (!isValidRegex(im)) {
                sendModMessage("Invalid pattern syntax");
                return -1;
            }
            if (regex.contains(im)) {
                sendModMessage("Already ignoring this pattern");
                return -1;
            }
            regex.add(im);
            sendModMessage("Ignoring messages matching ");
            return 1;
        })));

        node.then(literal("remove").then(argument("index", IntegerArgumentType.integer(1)).executes(c -> {
            int index = c.getArgument("index", Integer.class) - 1;
            if (index >= regex.size()) {
                Component prefix = Component.text("Index out of bounds. Use ", NamedTextColor.GOLD);
                Component suggestion = Component.text("/" + cmd + " list")
                        .style(Style.style(NamedTextColor.AQUA)
                                .hoverEvent(HoverEvent.hoverEvent(HoverEvent.Action.SHOW_TEXT, Component.text("click to execute")))
                                .clickEvent(ClickEvent.runCommand("/" + cmd + " list")));
                Component suffix = Component.text(" to see available indices.", NamedTextColor.GOLD);
                sendModMessage(prefix.append(suggestion).append(suffix));
                return -1;
            }
            sendModMessage(Component.text("Removed ", NamedTextColor.GOLD)
                    .append(Component.text(regex.remove(index), NamedTextColor.AQUA)));
            return 1;
        })));

        node.then(literal("list").executes(c -> {
            if (regex.isEmpty()) {
                sendModMessage("No regexes registered!");
                return 1;
            }
            sendModMessage("List of ignored message-regexes:");
            for (int i = 0; i < regex.size(); i++) {
                sendModMessage(Component.text("[" + (i + 1) + "] ", NamedTextColor.GOLD)
                        .append(Component.text(regex.get(i), NamedTextColor.AQUA)));
            }
            return 1;
        }));

        node.then(literal("clear").executes(c -> {
            regex.clear();
            sendModMessage("Cleared ignored messages");
            return 1;
        }));

        LiteralArgumentBuilder<ClientSuggestionProvider> predefined = literal("predefined");
        for (Predefined pre : Predefined.values()) {
            predefined.then(literal(pre.getKey()).executes(c -> {
                boolean disable = isEnabled(pre);
                if (disable) {
                    disable(pre);
                    sendModMessage(pre.getMessage() + " disabled");
                } else {
                    enable(pre);
                    sendModMessage(pre.getMessage() + " enabled");
                }
                return 1;
            }));
        }
        node.then(predefined);

        node.executes(c -> {
            sendDebugMessage();
            return 1;
        });

        register(node,
                "MessageIgnorer allows you to set specific Regular Expressions (also known as RegEX) which when matched are not displayed in your chat.",
                "The predefined values contain useful types of messages like all messages sent by the adminshop when selling items or vote-rewards of other players.");
    }

    private boolean isEnabled(Predefined pre) {
        for (String string : pre.getRegexes()) {
            if (regex.contains(string)) return true;
        }
        return false;
    }

    public void disable(Predefined pre) {
        for (String s : pre.getRegexes()) {
            regex.remove(s);
        }
    }

    public void enable(Predefined pre) {
        for (String s : pre.getRegexes()) {
            if (!regex.contains(s))
                regex.add(s);
        }
    }

    private void sendDebugMessage() {
        sendModMessage("/ignoremessage [add,remove,clear,list,test,toggle]");
    }

    private boolean isValidRegex(String s) {
        try {
            Pattern.compile(s);
            return true;
        } catch (PatternSyntaxException e) {
            return false;
        }
    }

    private void onChat(AddChatMessageCallback.ChatAddEvent event) {
        if (!enabled) {
            return;
        }
        String s = PlayerUtils.removeColorCodes(event.getChatText().getString());
        for (String match : regex) {
            if (s.matches(match)) {
                event.setCanceled(true);
                return;
            }
        }
    }
}
