package at.haha007.edenclient.mods;

import at.haha007.edenclient.callbacks.AddChatMessageCallback;
import at.haha007.edenclient.callbacks.ConfigLoadCallback;
import at.haha007.edenclient.callbacks.ConfigSaveCallback;
import at.haha007.edenclient.utils.MathUtils;
import at.haha007.edenclient.utils.PlayerUtils;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.client.network.ClientCommandSource;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.text.LiteralText;
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
            PlayerUtils.sendModMessage(new LiteralText((enabled = !enabled) ? "Message ignoring enabled" : "Message ignoring disabled").formatted(Formatting.GOLD));
            return 1;
        }));
        node.then(literal("add").then(argument("regex", StringArgumentType.greedyString()).executes(c -> {
            String im = c.getArgument("regex", String.class);
            if (!isValidRegex(im)) {
                PlayerUtils.sendModMessage(new LiteralText("Invalid pattern syntax").formatted(Formatting.GOLD));
                return 1;
            }
            regex.add(im);
            PlayerUtils.sendModMessage(new LiteralText("Ignoring messages matching " + im).formatted(Formatting.GOLD));
            return 1;
        })));
        node.then(literal("remove").then(argument("regex", StringArgumentType.greedyString()).executes(c -> {
            String im = c.getArgument("regex", String.class);
            if (MathUtils.isInteger(im) && !regex.contains(im)) {
                int index = Integer.parseInt(im);
                if (removeIndex(index)) {
                    PlayerUtils.sendModMessage(new LiteralText("Removed ignored message at index: ").formatted(Formatting.GOLD).append(new LiteralText(Integer.toString(index))).formatted(Formatting.AQUA));
                } else {
                    PlayerUtils.sendModMessage(new LiteralText("Index out of bounds: " + index + " ->").formatted(Formatting.GOLD).append(new LiteralText(" allowed indices: " + 1 + " to " + regex.size()).formatted(Formatting.AQUA)));
                }
                return 1;
            }
            if (regex.remove(im)) {
                PlayerUtils.sendModMessage(new LiteralText("Removed ignored message ").formatted(Formatting.GOLD).append(new LiteralText(im).formatted(Formatting.AQUA)));
            } else {
                PlayerUtils.sendModMessage(new LiteralText("Could not find message or message at index: ").formatted(Formatting.GOLD).append(new LiteralText(im).formatted(Formatting.AQUA)));
            }
            return 1;
        })));
        node.then(literal("list").executes(c -> {
            if (regex.isEmpty()) {
                PlayerUtils.sendModMessage(new LiteralText("No regexes registered!").formatted(Formatting.GOLD));
                return 1;
            }
            PlayerUtils.sendModMessage(new LiteralText("List of ignored message-regexes:").formatted(Formatting.GOLD));
            for (int i = 0; i < regex.size(); i++) {
                PlayerUtils.sendModMessage(new LiteralText("[" + (i + 1) + "] " + regex.get(i)).formatted(Formatting.GOLD));
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

    private boolean removeIndex(int index) {
        if (index < 1 || index > regex.size()) {
            return false;
        }
        regex.remove(index - 1);
        return true;
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
