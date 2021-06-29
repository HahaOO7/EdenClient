package at.haha007.edenclient.mods.wordhighlighter;

import at.haha007.edenclient.command.Command;
import at.haha007.edenclient.command.CommandManager;
import at.haha007.edenclient.utils.PlayerUtils;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;
import org.apache.logging.log4j.core.pattern.FormattingInfo;
import sun.tools.jstat.Literal;

import java.util.*;

public class WordHighlighter {

    private static Set<String> words = new HashSet<>();

    public WordHighlighter() {
        CommandManager.registerCommand(new Command(this::onCommand), "highlight", "hl");
    }

    private void onCommand(Command command, String label, String[] inputs) {
        if (inputs.length < 1) {
            WordHighlighterUtils.sendDebugMessage();
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

        message = createColoredMessage(formattedMsg, message);

        return message;
    }

    private static Text createColoredMessage(String formattedMsg, Text message) {
        MutableText returnText = new LiteralText("");

        Style style = Style.EMPTY;

        for (int i = 0; i < formattedMsg.length(); i++) {
            MutableText current = new LiteralText("");
            if (formattedMsg.charAt(i) == '§') {
                style = WordHighlighterUtils.getCurrentStyle(style, formattedMsg.charAt(i + 1));
                i++;
            }
            current.setStyle(style);
            current.append("" + formattedMsg.charAt(i));
            returnText.append(current);
        }
        return returnText;
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
