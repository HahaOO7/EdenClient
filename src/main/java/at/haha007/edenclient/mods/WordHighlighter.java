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
import net.minecraft.text.*;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WordHighlighter {

    protected static Set<String> words = new HashSet<>();
    protected static boolean enabled;
    private final Style style = Style.EMPTY.withBold(true).withUnderline(true).withColor(Formatting.AQUA);

    public WordHighlighter() {
        CommandManager.registerCommand(new Command(this::onCommand), "highlight", "hl");
        AddChatMessageCallback.EVENT.register(this::onChat);
        ConfigSaveCallback.EVENT.register(this::onSave);
        ConfigLoadCallback.EVENT.register(this::onLoad);
    }

    private ActionResult onChat(AddChatMessageCallback.ChatAddEvent event) {
        words.forEach(string -> event.setChatText(highlight(event.getChatText(), string)));
        return ActionResult.PASS;
    }

    private ActionResult onLoad(NbtCompound compoundTag) {
        NbtCompound tag = compoundTag.getCompound("wordhighlighter");

        if (tag == null) {
            WordHighlighter.words = new HashSet<>();
            return ActionResult.PASS;
        }

        if (!tag.contains("enabled")) {
            WordHighlighter.enabled = false;
        } else {
            WordHighlighter.enabled = tag.getBoolean("enabled");
        }

        NbtList nbtList = tag.getList("words", 8);

        WordHighlighter.words = new HashSet<>();
        if (nbtList != null) {
            for (NbtElement tag1 : nbtList) {
                WordHighlighter.words.add(tag1.asString());
            }
        }

        return ActionResult.PASS;
    }

    private ActionResult onSave(NbtCompound compoundTag) {
        NbtCompound tag = new NbtCompound();
        tag.putBoolean("enabled", WordHighlighter.enabled);
        NbtList nbtList = new NbtList();
        for (String word : WordHighlighter.words) {
            nbtList.add(NbtString.of(word));
        }
        tag.put("words", nbtList);
        compoundTag.put("wordhighlighter", tag);

        return ActionResult.PASS;
    }

    private void onCommand(Command command, String label, String[] inputs) {
        if (inputs.length < 1) {

            enabled = !enabled;
            if (enabled)
                PlayerUtils.sendMessage(new LiteralText("[EdenClient] Enabled WordHighlighter!").formatted(Formatting.GOLD));
            else
                PlayerUtils.sendMessage(new LiteralText("[EdenClient] Disabled WordHighlighter!").formatted(Formatting.GOLD));

            PlayerUtils.sendMessage(new LiteralText("[EdenClient] For further usage use one of the following commands:").formatted(Formatting.GOLD));
            sendUsageDebugMessage();
            return;
        }

        for (int i = 0; i < inputs.length; i++) {
            inputs[i] = inputs[i].toLowerCase();
        }

        switch (inputs[0]) {
            case "add" -> addWords(inputs);
            case "remove" -> removeWords(inputs);
            case "list" -> listWords();
            case "clear" -> clearWords();
            default -> sendDebugMessage();
        }
    }

    private void listWords() {
        PlayerUtils.sendMessage(new LiteralText("[EdenClient] These words are currently highlighted:").formatted(Formatting.GOLD));
        PlayerUtils.sendMessage(new LiteralText("[EdenClient] " + WordHighlighter.words.toString()).formatted(Formatting.GOLD));
    }

    private void clearWords() {
        words.clear();
        PlayerUtils.sendMessage(new LiteralText("[EdenClient] Cleared all words!").formatted(Formatting.GOLD));
    }

    private void addWords(String[] inputs) {
        if (inputs.length < 2) {
            sendDebugMessage();
        }
        PlayerUtils.sendMessage(new LiteralText("[EdenClient] Added words!").formatted(Formatting.GOLD));
        WordHighlighter.words.addAll(Arrays.asList(inputs).subList(1, inputs.length));
    }

    private void removeWords(String[] inputs) {
        if (inputs.length < 2) {
            sendDebugMessage();
        }
        Arrays.asList(inputs).subList(1, inputs.length).forEach(WordHighlighter.words::remove);
        PlayerUtils.sendMessage(new LiteralText("[EdenClient] Removed words (if viable)!").formatted(Formatting.GOLD));
    }


    private void sendUsageDebugMessage() {
        PlayerUtils.sendMessage(new LiteralText("[EdenClient] Using only \"/hl\" or \"/highlight\" will toggle the WordHighlighter!").formatted(Formatting.GOLD));
        PlayerUtils.sendMessage(new LiteralText("[EdenClient] You may use one of the following arguments: [add, remove, clear, list]!").formatted(Formatting.GOLD));
        PlayerUtils.sendMessage(new LiteralText("[EdenClient] E.G: /highlights add EmielRegis").formatted(Formatting.GOLD));
    }

    private void sendDebugMessage() {
        PlayerUtils.sendMessage(new LiteralText("[EdenClient] Wrong use of command!").formatted(Formatting.GOLD));
        PlayerUtils.sendMessage(new LiteralText("[EdenClient] Using only \"/hl\" or \"/highlight\" will toggle the WordHighlighter!").formatted(Formatting.GOLD));
        PlayerUtils.sendMessage(new LiteralText("[EdenClient] You may use one of the following arguments: [add, remove, clear, list]!").formatted(Formatting.GOLD));
        PlayerUtils.sendMessage(new LiteralText("[EdenClient] E.G: /highlights add EmielRegis").formatted(Formatting.GOLD));
    }

    private Text highlight(Text text, String string) {
        text.asOrderedText();
        if (text instanceof LiteralText t) {
            String s = t.getRawString();
            Pattern pattern = Pattern.compile(string, Pattern.CASE_INSENSITIVE | Pattern.LITERAL);
            Matcher matcher = pattern.matcher(s);
            List<MutableText> subtext = new ArrayList<>();
            Style baseStyle = t.getStyle();
            Style style = this.style.withHoverEvent(baseStyle.getHoverEvent()).withClickEvent(baseStyle.getClickEvent());
            while (matcher.find()) {
                int start = matcher.start();
                int end = matcher.end();
                String pre = s.substring(0, start);
                String match = s.substring(start, end);
                if (!pre.isEmpty())
                    subtext.add(new LiteralText(pre).setStyle(baseStyle));
                subtext.add(new LiteralText(match).setStyle(style));
                s = s.substring(end);
                matcher = pattern.matcher(s);
            }
            if (!s.isEmpty())
                subtext.add(new LiteralText(s).setStyle(baseStyle));
            MutableText nextText = new LiteralText("");
            subtext.forEach(nextText::append);
            t.getSiblings().stream().map(sibling -> highlight(sibling, string)).forEach(nextText::append);
            return nextText;
        } else if (text instanceof TranslatableText t) {
            Object[] args = t.getArgs();
            for (int i = 0; i < args.length; i++) {
                if (args[i] instanceof Text y)
                    args[i] = highlight(y, string);
                else if (args[i] instanceof String y)
                    args[i] = highlight(new LiteralText(y), string);
            }
            t.getSiblings().replaceAll(y -> highlight(y, string));
            return t;
        } else {
            List<Text> next = text.getSiblings().stream().map(x -> highlight(x, string)).toList();
            text.getSiblings().clear();
            text.getSiblings().addAll(next);
            return text;
        }
    }
}
