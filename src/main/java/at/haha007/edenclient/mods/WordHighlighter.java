package at.haha007.edenclient.mods;

import at.haha007.edenclient.callbacks.AddChatMessageCallback;
import at.haha007.edenclient.callbacks.ConfigLoadCallback;
import at.haha007.edenclient.callbacks.ConfigSaveCallback;
import at.haha007.edenclient.command.Command;
import at.haha007.edenclient.command.CommandManager;
import at.haha007.edenclient.utils.MathUtils;
import at.haha007.edenclient.utils.PlayerUtils;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.text.*;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WordHighlighter {

    protected static List<String> words = new ArrayList<>();
    protected static boolean enabled, italic, bold, underlined;
    protected static Color color;
    protected static int r = 0, g = 0, b = 0, baseColor = 5636095;

    private static Style style = Style.EMPTY;

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
            WordHighlighter.words = new ArrayList<>();
            return ActionResult.PASS;
        }

        if (!tag.contains("enabled")) {
            WordHighlighter.enabled = false;
        } else {
            WordHighlighter.enabled = tag.getBoolean("enabled");
        }

        NbtList nbtList = tag.getList("words", 8);

        WordHighlighter.words = new ArrayList<>();
        if (nbtList != null) {
            for (NbtElement tag1 : nbtList) {
                WordHighlighter.words.add(tag1.asString());
            }
        }

        if (!tag.contains("bold"))
            WordHighlighter.bold = false;
        else
            WordHighlighter.bold = tag.getBoolean("bold");

        if (!tag.contains("italic"))
            WordHighlighter.italic = false;
        else
            WordHighlighter.italic = tag.getBoolean("italic");

        if (!tag.contains("underlined"))
            WordHighlighter.underlined = false;
        else
            WordHighlighter.underlined = tag.getBoolean("underlined");

        if (!tag.contains("r") || !tag.contains("g") || !tag.contains("b"))
            WordHighlighter.color = new Color(baseColor);
        else
            WordHighlighter.color = new Color(tag.getInt("r"), tag.getInt("g"), tag.getInt("b"));

        style = Style.EMPTY.withColor(TextColor.fromRgb(color.getRGB())).withUnderline(underlined).withBold(bold).withItalic(italic);

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

        tag.putBoolean("bold", bold);
        tag.putBoolean("italic", italic);
        tag.putBoolean("underlined", underlined);
        tag.putInt("r", color.getRed());
        tag.putInt("g", color.getGreen());
        tag.putInt("b", color.getBlue());

        return ActionResult.PASS;
    }

    private void onCommand(Command command, String label, String[] inputs) {
        if (inputs.length < 1) {

            enabled = !enabled;
            if (enabled)
                PlayerUtils.sendModMessage(new LiteralText("Enabled WordHighlighter!").formatted(Formatting.GOLD));
            else
                PlayerUtils.sendModMessage(new LiteralText("Disabled WordHighlighter!").formatted(Formatting.GOLD));

            PlayerUtils.sendModMessage(new LiteralText("For further usage use one of the following commands:").formatted(Formatting.GOLD));
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
            case "bold" -> setBold(!bold);
            case "italic" -> setItalic(!italic);
            case "underline", "underlined" -> setUnderlined(!underlined);
            case "style" -> setStyle(inputs);
            case "color", "rgb" -> setColor(inputs);
            default -> sendDebugMessage();
        }
    }

    private void setBold(boolean bold) {
        WordHighlighter.bold = bold;
        PlayerUtils.sendModMessage(bold ? new LiteralText("Words are now bold!") : new LiteralText("Words are no longer bold!"));
    }

    public static void setItalic(boolean italic) {
        WordHighlighter.italic = italic;
        PlayerUtils.sendModMessage(italic ? new LiteralText("Words are now italic!") : new LiteralText("Words are no longer italic!"));
    }

    public static void setUnderlined(boolean underlined) {
        WordHighlighter.underlined = underlined;
        PlayerUtils.sendModMessage(underlined ? new LiteralText("Words are now underlined!") : new LiteralText("Words are no longer underlined!"));
    }

    private void setColor(String[] inputs) {
        if (inputs.length == 2) {
            if (MathUtils.isInteger(inputs[1])) {
                color = new Color(Integer.parseInt(inputs[1]));
                PlayerUtils.sendModMessage(new LiteralText("New color set from RGB value!"));
            } else {
                color = getColorFromColorCode(inputs[1].trim());
                PlayerUtils.sendModMessage(new LiteralText("New color set from Bukkit ColorCode values!"));
            }
            return;
        }

        if (inputs.length == 4) {
            if (MathUtils.isInteger(inputs[1]))
                r = Integer.parseInt(inputs[1]) % 256;
            if (MathUtils.isInteger(inputs[2]))
                g = Integer.parseInt(inputs[2]) % 256;
            if (MathUtils.isInteger(inputs[3]))
                b = Integer.parseInt(inputs[3]) % 256;
            PlayerUtils.sendModMessage(new LiteralText("New color set from RGB values!"));
            return;
        }
        sendDebugMessage();
    }

    private void setStyle(String[] inputs) {
        if (inputs[1].equals("reset")) {
            style = Style.EMPTY.withColor(TextColor.fromRgb(5636095));
            PlayerUtils.sendModMessage(new LiteralText("Style reset!"));
        }

        if (inputs.length == 2) {
            if (inputs[1].contains("&"))
                style = getStyleFromFormattingCode(inputs[1].trim());
            PlayerUtils.sendModMessage(new LiteralText("Style set from Bukkit FormattingCodes!"));
        }
    }

    private static Style getStyleFromFormattingCode(String input) {
        Style style = Style.EMPTY;
        String[] inputs = input.replace("&", " &").trim().split(" ");

        for (String s : inputs) {
            switch (s) {
                case "&r" -> style = Style.EMPTY.withColor(TextColor.fromRgb(baseColor));
                case "&l" -> style = style.withBold(true);
                case "&n" -> style = style.withUnderline(true);
                case "&o" -> style = style.withItalic(true);
                case "&1" -> style = style.withColor(TextColor.fromRgb(170));
                case "&2" -> style = style.withColor(TextColor.fromRgb(43520));
                case "&3" -> style = style.withColor(TextColor.fromRgb(43690));
                case "&4" -> style = style.withColor(TextColor.fromRgb(11141120));
                case "&5" -> style = style.withColor(TextColor.fromRgb(11141290));
                case "&6" -> style = style.withColor(TextColor.fromRgb(16755200));
                case "&7" -> style = style.withColor(TextColor.fromRgb(11184810));
                case "&8" -> style = style.withColor(TextColor.fromRgb(5592405));
                case "&9" -> style = style.withColor(TextColor.fromRgb(5592575));
                case "&a" -> style = style.withColor(TextColor.fromRgb(5635925));
                case "&b" -> style = style.withColor(TextColor.fromRgb(5636095));
                case "&c" -> style = style.withColor(TextColor.fromRgb(16733525));
                case "&d" -> style = style.withColor(TextColor.fromRgb(16733695));
                case "&e" -> style = style.withColor(TextColor.fromRgb(16777045));
                case "&f" -> style = style.withColor(TextColor.fromRgb(16777215));
                default -> {
                }
            }
        }
        return style;
    }

    private static Color getColorFromColorCode(String input) {
        Color color = WordHighlighter.color;
        String[] inputs = input.replace("&", " &").trim().split(" ");

        for (String s : inputs) {
            switch (s) {
                case "&1" -> color = new Color(170);
                case "&2" -> color = new Color(43520);
                case "&3" -> color = new Color(43690);
                case "&4" -> color = new Color(11141120);
                case "&5" -> color = new Color(11141290);
                case "&6" -> color = new Color(16755200);
                case "&7" -> color = new Color(11184810);
                case "&8" -> color = new Color(5592405);
                case "&9" -> color = new Color(5592575);
                case "&a" -> color = new Color(5635925);
                case "&b" -> color = new Color(5636095);
                case "&c" -> color = new Color(16733525);
                case "&d" -> color = new Color(16733695);
                case "&e" -> color = new Color(16777045);
                case "&f" -> color = new Color(16777215);
                default -> {
                }
            }
        }
        return color;
    }

    private void listWords() {
        PlayerUtils.sendModMessage(new LiteralText("These words are currently highlighted:").formatted(Formatting.GOLD));
        PlayerUtils.sendModMessage(new LiteralText(WordHighlighter.words.toString()).formatted(Formatting.GOLD));
    }

    private void clearWords() {
        words.clear();
        PlayerUtils.sendModMessage(new LiteralText("Cleared all words!").formatted(Formatting.GOLD));
    }

    private void addWords(String[] inputs) {
        if (inputs.length < 2) {
            sendDebugMessage();
        }
        PlayerUtils.sendModMessage(new LiteralText("Added words!").formatted(Formatting.GOLD));
        WordHighlighter.words.addAll(Arrays.asList(inputs).subList(1, inputs.length));
        words.sort(Comparator.comparingInt(String::length).reversed());
    }

    private void removeWords(String[] inputs) {
        if (inputs.length < 2) {
            sendDebugMessage();
        }
        Arrays.asList(inputs).subList(1, inputs.length).forEach(WordHighlighter.words::remove);
        PlayerUtils.sendModMessage(new LiteralText("Removed words (if viable)!").formatted(Formatting.GOLD));
    }

    private void sendUsageDebugMessage() {
        PlayerUtils.sendModMessage(new LiteralText("Using only \"/hl\" or \"/highlight\" will toggle the WordHighlighter!").formatted(Formatting.GOLD));
        PlayerUtils.sendModMessage(new LiteralText("You may use one of the following arguments: [add <words>, remove <words>, clear, list, bold, italic, underline, style <formattingcode/reset>, color <colorcode/rgb>]!").formatted(Formatting.GOLD));
        PlayerUtils.sendModMessage(new LiteralText("E.G: /highlights add EmielRegis").formatted(Formatting.GOLD));
    }

    private void sendDebugMessage() {
        PlayerUtils.sendModMessage(new LiteralText("Wrong use of command!").formatted(Formatting.GOLD));
        PlayerUtils.sendModMessage(new LiteralText("Using only \"/hl\" or \"/highlight\" will toggle the WordHighlighter!").formatted(Formatting.GOLD));
        PlayerUtils.sendModMessage(new LiteralText("You may use one of the following arguments: [add <words>, remove <words>, clear, list, bold, italic, underline, style <formattingcode/reset>, color <colorcode/rgb>]!").formatted(Formatting.GOLD));
        PlayerUtils.sendModMessage(new LiteralText("E.G: /highlights add EmielRegis").formatted(Formatting.GOLD));
    }

    private Text highlight(Text text, String string) {
        text.asOrderedText();
        if (text instanceof LiteralText t) {
            String s = t.getRawString();
            Pattern pattern = Pattern.compile(string, Pattern.CASE_INSENSITIVE | Pattern.LITERAL);
            Matcher matcher = pattern.matcher(s);
            List<MutableText> subtext = new ArrayList<>();
            Style baseStyle = t.getStyle();
            Style style = WordHighlighter.style.withHoverEvent(baseStyle.getHoverEvent()).withClickEvent(baseStyle.getClickEvent());
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
