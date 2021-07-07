package at.haha007.edenclient.mods;

import at.haha007.edenclient.callbacks.AddChatMessageCallback;
import at.haha007.edenclient.callbacks.ConfigLoadCallback;
import at.haha007.edenclient.callbacks.ConfigSaveCallback;
import at.haha007.edenclient.command.Command;
import at.haha007.edenclient.command.CommandManager;
import at.haha007.edenclient.utils.PlayerUtils;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.text.LiteralText;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class MessageIgnorer {
    private final List<String> regex = new ArrayList<>();
    private boolean enabled;

    public MessageIgnorer() {
        AddChatMessageCallback.EVENT.register(this::onChat);
        CommandManager.registerCommand(new Command(this::onCommand), "ignoremessage", "im");
        ConfigSaveCallback.EVENT.register(this::onSave);
        ConfigLoadCallback.EVENT.register(this::onLoad);
    }

    private ActionResult onLoad(NbtCompound nbtCompound) {
        regex.clear();
        enabled = false;
        if (!nbtCompound.contains("MessegeIgnorer")) {
            return ActionResult.PASS;
        }
        NbtCompound tag = nbtCompound.getCompound("MessegeIgnorer");
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
        tag.putBoolean("enabled", true);
        NbtList list = new NbtList();
        regex.forEach(s -> list.add(NbtString.of(s)));
        tag.put("regex", list);
        nbtCompound.put("MessegeIgnorer", tag);
        return ActionResult.PASS;
    }

    private void onCommand(Command cmd, String label, String[] args) {
        if (args.length == 0) {
            sendDebugMessage();
            return;
        }
        switch (args[0].toLowerCase()) {
            case "toggle" -> PlayerUtils.sendModMessage(new LiteralText((enabled = !enabled) ? "Message ignoring enabled" : "Message ignoring disabled").formatted(Formatting.GOLD));
            case "add" -> {
                if (args.length < 2) {
                    PlayerUtils.sendModMessage(new LiteralText("/ignoremessage add <message>").formatted(Formatting.GOLD));
                    return;
                }
                String ignore = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
                try {
                    Pattern.compile(ignore);
                } catch (PatternSyntaxException e) {
                    PlayerUtils.sendModMessage(new LiteralText("Invalid pattern syntax").formatted(Formatting.GOLD));
                    return;
                }
                regex.add(ignore);
                PlayerUtils.sendModMessage(new LiteralText("Ignoring messages matching " + ignore).formatted(Formatting.GOLD));
            }
            case "remove" -> {
                if (args.length < 2) {
                    PlayerUtils.sendModMessage(new LiteralText("/ignoremessage remove <message>").formatted(Formatting.GOLD));
                    return;
                }
                String ignore = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
                if (regex.remove(ignore)) {
                    PlayerUtils.sendModMessage(new LiteralText("Removed ignored message").formatted(Formatting.GOLD));
                } else {
                    PlayerUtils.sendModMessage(new LiteralText("Could not find message: " + ignore).formatted(Formatting.GOLD));
                }
            }
            case "list" -> PlayerUtils.sendModMessage(new LiteralText(Arrays.toString(regex.toArray())).formatted(Formatting.GOLD));
            case "clear" -> {
                regex.clear();
                PlayerUtils.sendModMessage(new LiteralText("Cleared ignored messages").formatted(Formatting.GOLD));
            }
            case "test" -> PlayerUtils.sendMessage(new LiteralText(String.join(" ", Arrays.copyOfRange(args, 1, args.length))));
        }
    }

    private void sendDebugMessage() {
        PlayerUtils.sendModMessage(new LiteralText("/ignoremessage [add,remove,clear,list,test,toggle]"));
    }

    private ActionResult onChat(AddChatMessageCallback.ChatAddEvent event) {
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
