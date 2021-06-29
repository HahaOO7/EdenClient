package at.haha007.edenclient.mods.wordhighlighter;

import at.haha007.edenclient.callbacks.ConfigLoadCallback;
import at.haha007.edenclient.callbacks.ConfigSaveCallback;
import at.haha007.edenclient.command.Command;
import at.haha007.edenclient.command.CommandManager;
import at.haha007.edenclient.utils.PlayerUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.text.*;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;

import java.util.*;

public class WordHighlighter {

    protected static Set<String> words = new HashSet<>();
    protected static boolean enabled;

    public WordHighlighter() {
        CommandManager.registerCommand(new Command(this::onCommand), "highlight", "hl");
        ConfigSaveCallback.EVENT.register(this::onSave);
        ConfigLoadCallback.EVENT.register(this::onLoad);
    }

    private ActionResult onLoad(CompoundTag compoundTag) {
        return WordHighlighterUtils.onLoad(compoundTag);
    }

    private ActionResult onSave(CompoundTag compoundTag) {
        return WordHighlighterUtils.onSave(compoundTag);
    }

    private void onCommand(Command command, String label, String[] inputs) {
        if (inputs.length < 1) {

            enabled = !enabled;
            if (enabled)
                PlayerUtils.sendMessage(new LiteralText("[EdenClient] Enabled WordHighlighter!").formatted(Formatting.GOLD));
            else
                PlayerUtils.sendMessage(new LiteralText("[EdenClient] Disabled WordHighlighter!").formatted(Formatting.GOLD));

            PlayerUtils.sendMessage(new LiteralText("[EdenClient] For further usage use one of the following commands:").formatted(Formatting.GOLD));
            WordHighlighterUtils.sendUsageDebugMessage();
            return;
        }

        for (int i = 0; i < inputs.length; i++) {
            inputs[i] = inputs[i].toLowerCase();
        }

        switch (inputs[0]) {
            case "add":
                PlayerUtils.sendMessage(new LiteralText("[EdenClient] Added words!").formatted(Formatting.GOLD));
                WordHighlighterUtils.addWords(inputs);
                break;
            case "remove":
                PlayerUtils.sendMessage(new LiteralText("[EdenClient] Removed words (if viable)!").formatted(Formatting.GOLD));
                WordHighlighterUtils.removeWords(inputs);
                break;
            case "list":
                WordHighlighterUtils.listWords();
                break;
            case "clear":
                WordHighlighterUtils.clearWords();
                break;
            default:
                WordHighlighterUtils.sendDebugMessage();
        }
    }

    public static Text formatMessage(Text message) {
        if (!enabled)
            return message;

        if (message.getString().startsWith("[EdenClient] ")) {
            return message;
        }

        if (!WordHighlighterUtils.shouldHighlight(message.getString().toLowerCase(), words)) {
            return message;
        }

        String formattedMsg = stringifyMessage(message);

        message = createColoredMessage(formattedMsg);

        return message;
    }

    private static String stringifyMessage(Text text) {
        StringBuilder s = new StringBuilder();
        s.append(WordHighlighterUtils.getStyleString(text.getStyle()));

        if (text.getSiblings().isEmpty())
            s.append(text.getString());

        for (Text sibling : text.getSiblings()) {
            s.append(stringifyMessage(sibling));
        }

        return s.toString();
    }

    private static Text createColoredMessage(String formattedMsg) {
        StringBuilder copiedMessage = new StringBuilder();
        Style oldStyle;
        Style style = Style.EMPTY;

        for (int i = 0; i < formattedMsg.length(); i++) {
            if (formattedMsg.charAt(i) == '§') {
                oldStyle = style;
                style = WordHighlighterUtils.getCurrentStyle(style, formattedMsg.charAt(i + 1));
                if (!style.equals(oldStyle))
                    copiedMessage.append(WordHighlighterUtils.getStyleString(style));
                i += 2;
            }

            String word = WordHighlighterUtils.wordShouldBeHighlighted(formattedMsg.substring(i));
            if (word != null) {
                copiedMessage.append("§3").append(formattedMsg, i, i + word.length());
                copiedMessage.append(WordHighlighterUtils.getStyleString(style));
                i += (word.length() - 1);
            } else {
                copiedMessage.append(formattedMsg.charAt(i));
            }
        }
        copiedMessage = WordHighlighterUtils.fixDoubles(copiedMessage);

        return new TranslatableText(copiedMessage.toString());
    }
}
