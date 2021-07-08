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
import java.util.List;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WordHighlighter {

    private List<String> words = new ArrayList<>();
    private boolean enabled;

    private static Style style = Style.EMPTY;

    public WordHighlighter() {
        CommandManager.registerCommand(new Command(this::onCommand), "highlight", "hl");
        AddChatMessageCallback.EVENT.register(this::onChat);
        ConfigSaveCallback.EVENT.register(this::onSave);
        ConfigLoadCallback.EVENT.register(this::onLoad);
    }

    private ActionResult onChat(AddChatMessageCallback.ChatAddEvent event) {
        Text chatText = event.getChatText();
        if (chatText == null) return ActionResult.PASS;
        words.forEach(string -> event.setChatText(highlight(chatText, string)));
        return ActionResult.PASS;
    }

    private ActionResult onLoad(NbtCompound compoundTag) {
        NbtCompound tag = compoundTag.getCompound("wordhighlighter");
        style = Style.EMPTY.withFormatting(Formatting.AQUA, Formatting.BOLD);
        if (tag == null) {
            words = new ArrayList<>();
            return ActionResult.PASS;
        }
        if (!tag.contains("enabled")) {
            enabled = false;
        } else {
            enabled = tag.getBoolean("enabled");
        }
        NbtList nbtList = tag.getList("words", 8);
        words = new ArrayList<>();
        if (nbtList != null) {
            for (NbtElement tag1 : nbtList) {
                words.add(tag1.asString());
            }
            words.sort(Comparator.comparingInt(String::length).reversed());
        }
        if (tag.contains("bold"))
            style = style.withBold(tag.getBoolean("bold"));
        if (tag.contains("italic"))
            style = style.withItalic(tag.getBoolean("italic"));
        if (tag.contains("underlined"))
            style = style.withUnderline(tag.getBoolean("underlined"));
        if (tag.contains("color"))
            style = style.withColor(tag.getInt("color"));
        return ActionResult.PASS;
    }

    private ActionResult onSave(NbtCompound compoundTag) {
        NbtCompound tag = new NbtCompound();
        tag.putBoolean("enabled", enabled);
        NbtList nbtList = new NbtList();
        for (String word : words) {
            nbtList.add(NbtString.of(word));
        }
        tag.put("words", nbtList);
        tag.putBoolean("bold", style.isBold());
        tag.putBoolean("italic", style.isItalic());
        tag.putBoolean("underlined", style.isUnderlined());
        tag.putInt("color", Objects.requireNonNull(style.getColor()).getRgb());

        compoundTag.put("wordhighlighter", tag);
        return ActionResult.PASS;
    }

    private void onCommand(Command command, String label, String[] inputs) {
        if (inputs.length < 1) {
            sendUsageDebugMessage();
            return;
        }

        for (int i = 0; i < inputs.length; i++) {
            inputs[i] = inputs[i].toLowerCase();
        }

        switch (inputs[0]) {
            case "toggle" -> PlayerUtils.sendModMessage(new LiteralText((enabled = !enabled) ? "Enabled WordHighlighter!" : "Disabled WordHighlighter!").formatted(Formatting.GOLD));
            case "add" -> addWords(inputs);
            case "remove" -> removeWords(inputs);
            case "list" -> listWords();
            case "clear" -> clearWords();
            case "bold" -> setBold(!style.isBold());
            case "italic" -> setItalic(!style.isItalic());
            case "underline", "underlined" -> setUnderlined(!style.isUnderlined());
            case "style" -> setStyle(inputs);
            case "color", "rgb" -> setColor(inputs);
            default -> sendDebugMessage();
        }
    }

    private void setBold(boolean bold) {
        style = style.withBold(bold);
        PlayerUtils.sendModMessage(new LiteralText(bold ? "Words are now bold!" : "Words are no longer bold!").formatted(Formatting.GOLD));
    }

    public static void setItalic(boolean italic) {
        style = style.withItalic(italic);
        PlayerUtils.sendModMessage(new LiteralText(italic ? "Words are now italic!" : "Words are no longer italic!").formatted(Formatting.GOLD));
    }

    public static void setUnderlined(boolean underlined) {
        style = style.withUnderline(underlined);
        PlayerUtils.sendModMessage(new LiteralText(underlined ? "Words are now underlined!" : "Words are no longer underlined!").formatted(Formatting.GOLD));
    }

    private void setColor(String[] inputs) {
        if (inputs.length == 2) {
            if (MathUtils.isInteger(inputs[1])) {
                style = style.withColor(Integer.parseInt(inputs[1]));
                PlayerUtils.sendModMessage(new LiteralText("New color set from RGB value!").formatted(Formatting.GOLD));
            } else if (inputs[1].length() == 1) {
                Formatting f = Formatting.byCode(inputs[1].charAt(0));
                if (f == null || !f.isColor()) {
                    PlayerUtils.sendModMessage(new LiteralText("Could not parse color!").formatted(Formatting.GOLD));
                    return;
                }
                style = style.withFormatting(f);
                PlayerUtils.sendModMessage(new LiteralText("New color set from Bukkit ColorCode values!").formatted(Formatting.GOLD));
            } else {
                PlayerUtils.sendModMessage(new LiteralText("Could not parse color!").formatted(Formatting.GOLD));
            }
            return;
        }

        if (inputs.length == 4) {
            int r, g, b;
            if (MathUtils.isInteger(inputs[1]))
                r = MathUtils.clamp(Integer.parseInt(inputs[1]), 0, 255);
            else {
                PlayerUtils.sendModMessage(new LiteralText("Could not parse color!").formatted(Formatting.GOLD));
                return;
            }
            if (MathUtils.isInteger(inputs[2]))
                g = MathUtils.clamp(Integer.parseInt(inputs[2]), 0, 255);
            else {
                PlayerUtils.sendModMessage(new LiteralText("Could not parse color!").formatted(Formatting.GOLD));
                return;
            }
            if (MathUtils.isInteger(inputs[3]))
                b = MathUtils.clamp(Integer.parseInt(inputs[3]), 0, 255);
            else {
                PlayerUtils.sendModMessage(new LiteralText("Could not parse color!").formatted(Formatting.GOLD));
                return;
            }
            style = style.withColor(new Color(r, g, b).getRGB());
            PlayerUtils.sendModMessage(new LiteralText("New color set from RGB values!").formatted(Formatting.GOLD));
            return;
        }
        PlayerUtils.sendModMessage(new LiteralText("/hl color [ColorCode,<r g b>]").formatted(Formatting.GOLD));
    }

    private void setStyle(String[] inputs) {
        if (inputs.length != 2) {
            PlayerUtils.sendModMessage(new LiteralText("/hl style <style>").formatted(Formatting.GOLD));
            return;
        }
        if (inputs[1].equals("reset")) {
            style = Style.EMPTY.withFormatting(Formatting.AQUA, Formatting.BOLD);
            PlayerUtils.sendModMessage(new LiteralText("Style reset!").formatted(Formatting.GOLD));
            return;
        }
        style = getStyleFromFormattingCode(inputs[1].trim());
        PlayerUtils.sendModMessage(new LiteralText("Style set from FormattingCodes!").formatted(Formatting.GOLD));
    }

    private static Style getStyleFromFormattingCode(String input) {
        Style style = Style.EMPTY;
        style = style.withFormatting(input.chars().mapToObj(c -> (char) c).
                map(Formatting::byCode).filter(Objects::nonNull).toList().toArray(new Formatting[0]));
        return style;
    }

    private void listWords() {
        PlayerUtils.sendModMessage(new LiteralText("These words are currently highlighted:").formatted(Formatting.GOLD));
        PlayerUtils.sendModMessage(new LiteralText(words.toString()).formatted(Formatting.GOLD));
    }

    private void clearWords() {
        words.clear();
        PlayerUtils.sendModMessage(new LiteralText("Cleared all words!").formatted(Formatting.GOLD));
    }

    private void addWords(String[] inputs) {
        if (inputs.length < 2) {
            PlayerUtils.sendModMessage(new LiteralText("/hl add <words>"));
        }
        PlayerUtils.sendModMessage(new LiteralText("Added words!").formatted(Formatting.GOLD));
        words.addAll(Arrays.asList(inputs).subList(1, inputs.length));
        words.sort(Comparator.comparingInt(String::length).reversed());
    }

    private void removeWords(String[] inputs) {
        if (inputs.length < 2) {
            PlayerUtils.sendModMessage(new LiteralText("/hl remove <words>").formatted(Formatting.GOLD));
        }
        Arrays.asList(inputs).subList(1, inputs.length).forEach(words::remove);
        PlayerUtils.sendModMessage(new LiteralText("Removed words (if viable)!").formatted(Formatting.GOLD));
    }

    private void sendUsageDebugMessage() {
        PlayerUtils.sendModMessage(new LiteralText("Command usage:").formatted(Formatting.GOLD));
        PlayerUtils.sendModMessage(new LiteralText("/hl [add,remove,toggle,clear,list,bold,italic,underline,style,color]").formatted(Formatting.GOLD));
    }

    private void sendDebugMessage() {
        PlayerUtils.sendModMessage(new LiteralText("Wrong use of command!").formatted(Formatting.GOLD));
        sendUsageDebugMessage();
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
