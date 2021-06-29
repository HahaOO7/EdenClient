package at.haha007.edenclient.mods.wordhighlighter;

import at.haha007.edenclient.callbacks.ConfigLoadCallback;
import at.haha007.edenclient.callbacks.ConfigSaveCallback;
import at.haha007.edenclient.command.Command;
import at.haha007.edenclient.command.CommandManager;
import at.haha007.edenclient.utils.PlayerUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.text.*;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import org.apache.logging.log4j.core.pattern.FormattingInfo;
import sun.tools.jstat.Literal;

import java.util.*;
import java.util.stream.Collectors;

public class WordHighlighter {

    protected static Set<String> words = new HashSet<>();
    boolean enabled;

    public WordHighlighter() {
        CommandManager.registerCommand(new Command(this::onCommand), "highlight", "hl");
        ConfigSaveCallback.EVENT.register(this::onSave);
        ConfigLoadCallback.EVENT.register(this::onLoad);
    }

    private ActionResult onLoad(CompoundTag compoundTag) {
        CompoundTag tag = compoundTag.getCompound("wordhighlighter");

        if (tag == null) {
            words = new HashSet<>();
            return ActionResult.PASS;
        }

        if (!tag.contains("enabled")) {
            enabled = false;
        } else {
            enabled = tag.getBoolean("enabled");
        }

        ListTag listTag = tag.getList("words", 8);

        words = new HashSet<>();
        if (listTag != null) {
            for (Tag tag1 : listTag) {
                words.add(tag1.asString());
            }
        }

        return ActionResult.PASS;
    }

    private ActionResult onSave(CompoundTag compoundTag) {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("enabled", enabled);
        ListTag listTag = new ListTag();
        for (String word : words) {
            listTag.add(StringTag.of(word));
        }
        tag.put("words", listTag);
        compoundTag.put("wordhighlighter", tag);

        return ActionResult.PASS;
    }

    private void onCommand(Command command, String label, String[] inputs) {
        if (inputs.length < 1) {
            enabled = !enabled;
            if (enabled)
                PlayerUtils.sendMessage(new LiteralText("[Eden] Enabled WordHighlighter!").formatted(Formatting.GOLD));
            else
                PlayerUtils.sendMessage(new LiteralText("[Eden] Disabled WordHighlighter!").formatted(Formatting.GOLD));

            PlayerUtils.sendMessage(new LiteralText("[Eden] For further usage use one of the following commands:").formatted(Formatting.GOLD));
            WordHighlighterUtils.sendUsageDebugMessage();
            return;
        }

        for (int i = 0; i < inputs.length; i++) {
            inputs[i] = inputs[i].toLowerCase();
        }

        switch (inputs[0]) {
            case "add":
                PlayerUtils.sendMessage(new LiteralText("[Eden] Added words!").formatted(Formatting.GOLD));
                addWords(inputs);
                break;
            case "remove":
                PlayerUtils.sendMessage(new LiteralText("[Eden] Removed words (if viable)!").formatted(Formatting.GOLD));
                removeWords(inputs);
                break;
            case "list":
                listWords();
                break;
            default:
                WordHighlighterUtils.sendDebugMessage();
        }
    }

    public static Text formatMessage(Text message) {
        if (message.getString().startsWith("[Eden] ")) {
            return message;
        }

        if (!WordHighlighterUtils.shouldHighlight(message.getString().toLowerCase(), words)) {
            return message;
        }

        String formattedMsg = stringifyMessage(message);

        message = createColoredMessage(formattedMsg);

        return message;
    }

    private static Text createColoredMessage(String formattedMsg) {
        StringBuilder copiedMessage = new StringBuilder();
        Style style = Style.EMPTY;

        for (int i = 0; i < formattedMsg.length(); i++) {
            String word = WordHighlighterUtils.wordShouldBeHighlighted(formattedMsg.substring(i));
            if (word != null) {
                copiedMessage.append("§3").append(formattedMsg.substring(i, i + word.length()));
                copiedMessage.append(WordHighlighterUtils.getStyleString(style));
                i += (word.length() - 1);
            } else {
                copiedMessage.append(formattedMsg.charAt(i));
            }
        }
        return new LiteralText(copiedMessage.toString());
    }

    private static String stringifyMessage(Text text) {
        StringBuilder s = new StringBuilder();
        s.append(WordHighlighterUtils.getStyleString(text.getStyle()));
        s.append(text.getString());

        for (Text sibling : text.getSiblings()) {
            s.append(stringifyMessage(sibling));
        }

        return s.toString();
    }

    private void addWords(String[] inputs) {
        if (inputs.length < 2) {
            WordHighlighterUtils.sendDebugMessage();
        }

        words.addAll(Arrays.asList(inputs).subList(1, inputs.length));
    }

    private void removeWords(String[] inputs) {
        if (inputs.length < 2) {
            WordHighlighterUtils.sendDebugMessage();
        }

        Arrays.asList(inputs).subList(1, inputs.length).forEach(words::remove);
    }

    private static void listWords() {
        PlayerUtils.sendMessage(new LiteralText("[Eden] These words are currently highlighted:").formatted(Formatting.GOLD));
        PlayerUtils.sendMessage(new LiteralText("[Eden] " + words.toString()).formatted(Formatting.GOLD));
    }
}
