package at.haha007.edenclient.mods;

import at.haha007.edenclient.callbacks.AddChatMessageCallback;
import at.haha007.edenclient.callbacks.ConfigLoadCallback;
import at.haha007.edenclient.callbacks.ConfigSaveCallback;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.client.network.ClientCommandSource;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import static at.haha007.edenclient.command.CommandManager.*;
import static at.haha007.edenclient.utils.PlayerUtils.sendModMessage;

public class MessageIgnorer {
    private final List<String> regex = new ArrayList<>();
    private boolean enabled;

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
                "\\/iteminfo log \\(what's the item ID of LOG\\?\\) ?");

        private final String[] regexes;
        private final String key;
        private final String message;

        Predefined(String key, String message, String... regexes) {
            this.key = key;
            this.regexes = regexes;
            this.message = message;
        }

        public String getKey() {
            return key;
        }

        public String getMessage() {
            return message;
        }

        public String[] getRegexes() {
            return regexes;
        }
    }

    public MessageIgnorer() {
        AddChatMessageCallback.EVENT.register(this::onChat);
        registerCommand("ignoremessage");
        registerCommand("im");
        ConfigSaveCallback.EVENT.register(this::onSave);
        ConfigLoadCallback.EVENT.register(this::onLoad);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    private void registerCommand(String cmd) {
        LiteralArgumentBuilder<ClientCommandSource> node = literal(cmd);

        node.then(literal("toggle").executes(c -> {
            enabled = !enabled;
            String msg = enabled ? "Message ignoring enabled" : "Message ignoring disabled";
            sendModMessage(new LiteralText(msg).formatted(Formatting.GOLD));
            return 1;
        }));

        node.then(literal("add").then(argument("regex", StringArgumentType.greedyString()).executes(c -> {
            String im = c.getArgument("regex", String.class);
            if (!isValidRegex(im)) {
                sendModMessage(new LiteralText("Invalid pattern syntax").formatted(Formatting.GOLD));
                return -1;
            }
            if (regex.contains(im)) {
                sendModMessage(new LiteralText("Already ignoring this pattern").formatted(Formatting.GOLD));
                return -1;
            }
            regex.add(im);
            sendModMessage(new LiteralText("Ignoring messages matching " + im).formatted(Formatting.GOLD));
            return 1;
        })));

        node.then(literal("remove").then(argument("index", IntegerArgumentType.integer(1)).executes(c -> {
            int index = c.getArgument("index", Integer.class) - 1;
            if (index >= regex.size()) {
                MutableText prefix = new LiteralText("Index out of bounds. Use ").formatted(Formatting.GOLD);
                MutableText suggestion = new LiteralText("/" + cmd + " list").setStyle(Style.EMPTY.
                        withColor(Formatting.AQUA).
                        withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new LiteralText("click to execute"))).
                        withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/" + cmd + " list")));
                MutableText suffix = new LiteralText(" to see available indices.").formatted(Formatting.GOLD);
                sendModMessage(prefix.append(suggestion).append(suffix));
                return -1;
            }
            sendModMessage(new LiteralText("Removed: ").formatted(Formatting.GOLD).
                    append(new LiteralText(regex.remove(index)).formatted(Formatting.AQUA)));
            return 1;
        })));

        node.then(literal("list").executes(c -> {
            if (regex.isEmpty()) {
                sendModMessage(new LiteralText("No regexes registered!").formatted(Formatting.GOLD));
                return 1;
            }
            sendModMessage(new LiteralText("List of ignored message-regexes:").formatted(Formatting.GOLD));
            for (int i = 0; i < regex.size(); i++) {
                MutableText txt = new LiteralText(regex.get(i)).formatted(Formatting.AQUA);
                MutableText prefix = new LiteralText("[" + (i + 1) + "] ").formatted(Formatting.GOLD);
                sendModMessage(prefix.append(txt));
            }
            return 1;
        }));

        node.then(literal("clear").executes(c -> {
            regex.clear();
            sendModMessage(new LiteralText("Cleared ignored messages").formatted(Formatting.GOLD));
            return 1;
        }));

        LiteralArgumentBuilder<ClientCommandSource> predefined = literal("predefined");
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
        register(node);
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

    private void onLoad(NbtCompound nbtCompound) {
        regex.clear();
        enabled = false;

        if (!nbtCompound.contains("MessageIgnorer")) {
            return;
        }
        NbtCompound tag = nbtCompound.getCompound("MessageIgnorer");
        if (tag.contains("regex")) {
            NbtList list = tag.getList("regex", 8);
            for (NbtElement e : list) {
                regex.add(e.asString());
            }
        }

        if (tag.contains("enabled")) enabled = tag.getBoolean("enabled");

    }

    private void onSave(NbtCompound nbtCompound) {
        NbtCompound tag = new NbtCompound();
        tag.putBoolean("enabled", enabled);
        NbtList list = new NbtList();
        regex.forEach(s -> list.add(NbtString.of(s)));
        tag.put("regex", list);
        nbtCompound.put("MessageIgnorer", tag);
    }

    private void sendDebugMessage() {
        sendModMessage(new LiteralText("/ignoremessage [add,remove,clear,list,test,toggle]"));
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
        String s = event.getChatText().getString();
        for (String match : regex) {
            if (s.matches(match)) {
                event.setChatText(null);
                return;
            }
        }
    }
}
