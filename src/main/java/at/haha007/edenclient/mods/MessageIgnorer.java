package at.haha007.edenclient.mods;

import at.haha007.edenclient.callbacks.AddChatMessageCallback;
import at.haha007.edenclient.callbacks.ConfigLoadCallback;
import at.haha007.edenclient.callbacks.ConfigSaveCallback;
import at.haha007.edenclient.utils.PlayerUtils;
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

public class MessageIgnorer {
    private final List<String> regex = new ArrayList<>();
    private boolean enabled;

    public MessageIgnorer() {
        AddChatMessageCallback.EVENT.register(this::onChat);
        registerCommand("ignoremessage");
        registerCommand("im");
        ConfigSaveCallback.EVENT.register(this::onSave);
        ConfigLoadCallback.EVENT.register(this::onLoad);
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
        if (tag.contains("enabled")) {
            enabled = tag.getBoolean("enabled");
        }
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

    private void registerCommand(String cmd) {
        LiteralArgumentBuilder<ClientCommandSource> node = literal(cmd);
        node.then(literal("toggle").executes(c -> {
            String msg = (enabled = !enabled) ? "Message ignoring enabled" : "Message ignoring disabled";
            PlayerUtils.sendModMessage(new LiteralText(msg).formatted(Formatting.GOLD));
            return 1;
        }));
        node.then(literal("add").then(argument("regex", StringArgumentType.greedyString()).executes(c -> {
            String im = c.getArgument("regex", String.class);
            if (!isValidRegex(im)) {
                PlayerUtils.sendModMessage(new LiteralText("Invalid pattern syntax").formatted(Formatting.GOLD));
                return -1;
            }
            if (regex.contains(im)) {
                PlayerUtils.sendModMessage(new LiteralText("Already ignoring this pattern").formatted(Formatting.GOLD));
                return -1;
            }
            regex.add(im);
            PlayerUtils.sendModMessage(new LiteralText("Ignoring messages matching " + im).formatted(Formatting.GOLD));
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
                PlayerUtils.sendModMessage(prefix.append(suggestion).append(suffix));
                return -1;
            }
            PlayerUtils.sendModMessage(new LiteralText("Removed: ").formatted(Formatting.GOLD).
                    append(new LiteralText(regex.remove(index)).formatted(Formatting.AQUA)));
            return 1;
        })));
        node.then(literal("list").executes(c -> {
            if (regex.isEmpty()) {
                PlayerUtils.sendModMessage(new LiteralText("No regexes registered!").formatted(Formatting.GOLD));
                return 1;
            }
            PlayerUtils.sendModMessage(new LiteralText("List of ignored message-regexes:").formatted(Formatting.GOLD));
            for (int i = 0; i < regex.size(); i++) {
                MutableText txt = new LiteralText(regex.get(i)).formatted(Formatting.AQUA);
                MutableText prefix = new LiteralText("[" + (i + 1) + "] ").formatted(Formatting.GOLD);
                PlayerUtils.sendModMessage(prefix.append(txt));
            }
            return 1;
        }));
        node.then(literal("clear").executes(c -> {
            regex.clear();
            PlayerUtils.sendModMessage(new LiteralText("Cleared ignored messages").formatted(Formatting.GOLD));
            return 1;
        }));
        node.then(literal("test").then(argument("text", StringArgumentType.greedyString()).executes(c -> {
            PlayerUtils.sendModMessage(new LiteralText(c.getArgument("text", String.class)));
            return 1;
        })));
        node.executes(c -> {
            sendDebugMessage();
            return 1;
        });
        register(node);
    }

    private void sendDebugMessage() {
        PlayerUtils.sendModMessage(new LiteralText("/ignoremessage [add,remove,clear,list,test,toggle]"));
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
