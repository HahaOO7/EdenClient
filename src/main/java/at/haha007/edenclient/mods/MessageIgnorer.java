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
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import static at.haha007.edenclient.command.CommandManager.*;
import static at.haha007.edenclient.utils.PlayerUtils.sendModMessage;

public class MessageIgnorer {
    private final static List<String> regex = new ArrayList<>();
    private static boolean enabled;

    public MessageIgnorer() {
        AddChatMessageCallback.EVENT.register(this::onChat);
        registerCommand("ignoremessage");
        registerCommand("im");
        ConfigSaveCallback.EVENT.register(this::onSave);
        ConfigLoadCallback.EVENT.register(this::onLoad);
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static List<String> getRegexes() {
        return regex;
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
        var predefined = node.then(literal("predefined"));
        predefined.then(literal("sellmessages").executes(c -> {
            String autosellSyntax = "Verkauft für \\$(?<money>[0-9]{1,5}\\.?[0-9]{0,2}) \\((?<amount>[0-9,]{1,5}) (?<item>[a-zA-Z0-9_]{1,30}) Einheiten je \\$[0-9]{1,5}\\.?[0-9]{0,2}\\)";
            String autosellSyntax2 = "\\$[0-9]{1,5}\\.?[0-9]{0,2} wurden deinem Konto hinzugefügt\\.";
            String autosellSyntax3 = "Fehler: Du hast keine Berechtigung, diese benannten Gegenstände zu verkaufen: .*";

            boolean display = containsAny(getRegexes(), new String[]{autosellSyntax, autosellSyntax2, autosellSyntax3});

            String msg = display ? "Message ignoring for Sell Messages disabled" : "Message ignoring for Sell Messages enabled";
            sendModMessage(new LiteralText(msg).formatted(Formatting.GOLD));

            removeOrAddRegexes(new String[]{autosellSyntax, autosellSyntax2, autosellSyntax3}, display);
            return 1;
        }));

        predefined.then(literal("votemessages").executes(c -> {

            String votemessageSyntax = ". \\/vote . [A-Za-z0-9_]{1,16} hat [0-9]{2}min Flugzeit erhalten\\.";
            String votemessageSyntax2 = ". \\/vote . [A-Za-z0-9_]{1,16} hat [1-4]{1} VoteC.ins? erhalten\\.";

            boolean display = containsAny(getRegexes(), new String[]{votemessageSyntax, votemessageSyntax2});

            String msg = display ? "Message ignoring for Vote Messages disabled" : "Message ignoring for Vote Messages enabled";
            sendModMessage(new LiteralText(msg).formatted(Formatting.GOLD));

            removeOrAddRegexes(new String[]{votemessageSyntax, votemessageSyntax2}, display);
            return 1;
        }));

        predefined.then(literal("globalchat").executes(c -> {

            String globalChatSyntax = "\\w+ \\| ~?\\w+ > .*";

            boolean display = containsAny(getRegexes(), new String[]{globalChatSyntax});

            String msg = display ? "Message ignoring for Global Chat Messages disabled" : "Message ignoring for Global Chat Messages enabled";
            sendModMessage(new LiteralText(msg).formatted(Formatting.GOLD));

            removeOrAddRegexes(new String[]{globalChatSyntax}, display);
            return 1;
        }));

        predefined.then(literal("discordchat").executes(c -> {

            String discordChatSyntax = "[DC] [A-Za-z0-9]{1,16} > .*";

            boolean display = containsAny(getRegexes(), new String[]{discordChatSyntax});

            String msg = display ? "Message ignoring for Discord Chat Messages disabled" : "Message ignoring for Discord Chat Messages enabled";
            sendModMessage(new LiteralText(msg).formatted(Formatting.GOLD));

            removeOrAddRegexes(new String[]{discordChatSyntax}, display);
            return 1;
        }));

        predefined.then(literal("iteminfo").executes(c -> {

            String iteminfoSyntax = "Item Information: ?";
            String iteminfoSyntax2 = "Voller Name: (?<originalname>[A-Za-z0-9_ ]{1,40})";
            String iteminfoSyntax3 = "Shop Schild: (?<shortenedname>[A-Za-z0-9_ ]{1,40})";
            String iteminfoSyntax4 = "\\/iteminfo \\(what's the item in hand\\?\\) ?";
            String iteminfoSyntax5 = "\\/iteminfo log \\(what's the item ID of LOG\\?\\) ?";

            boolean display = containsAny(getRegexes(), new String[]{iteminfoSyntax, iteminfoSyntax2, iteminfoSyntax3, iteminfoSyntax4, iteminfoSyntax5});

            String msg = display ? "Message ignoring for ItemInfo messages disabled" : "Message ignoring for ItemInfo messages enabled";
            sendModMessage(new LiteralText(msg).formatted(Formatting.GOLD));

            removeOrAddRegexes(new String[]{iteminfoSyntax, iteminfoSyntax2, iteminfoSyntax3, iteminfoSyntax4, iteminfoSyntax5}, display);
            return 1;
        }));

        node.executes(c -> {
            sendDebugMessage();
            return 1;
        });
        register(node);
    }

    private ActionResult onLoad(NbtCompound nbtCompound) {
        regex.clear();
        enabled = false;

        if (!nbtCompound.contains("MessageIgnorer")) {
            return ActionResult.PASS;
        }
        NbtCompound tag = nbtCompound.getCompound("MessageIgnorer");
        if (tag.contains("regex")) {
            NbtList list = tag.getList("regex", 8);
            for (NbtElement e : list) {
                regex.add(e.asString());
            }
        }

        if (tag.contains("enabled")) enabled = tag.getBoolean("enabled");

        return ActionResult.PASS;
    }

    private ActionResult onSave(NbtCompound nbtCompound) {
        NbtCompound tag = new NbtCompound();
        tag.putBoolean("enabled", enabled);
        NbtList list = new NbtList();
        regex.forEach(s -> list.add(NbtString.of(s)));
        tag.put("regex", list);
        nbtCompound.put("MessageIgnorer", tag);
        return ActionResult.PASS;
    }

    private boolean containsAny(List<String> regexes, String[] strings) {
        for (String string : strings) {
            if (regexes.contains(string)) return true;
        }
        return false;
    }

    private void removeOrAddRegexes(String[] regexes, boolean display) {
        if (display) {
            for (String s : regexes) {
                regex.remove(s);
            }
        } else {
            for (String s : regexes) {
                if (!regex.contains(s))
                    regex.add(s);
            }
        }
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

    private ActionResult onChat(AddChatMessageCallback.ChatAddEvent event) {
        if (!enabled) {
            return ActionResult.PASS;
        }
        String s = event.getChatText().getString();
        for (String match : regex) {
            if (s.matches(match)) {
                event.setChatText(null);
                return ActionResult.PASS;
            }
        }
        return ActionResult.PASS;
    }
}
