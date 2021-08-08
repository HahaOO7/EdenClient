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
import net.minecraft.util.Formatting;

import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static at.haha007.edenclient.command.CommandManager.*;
import static at.haha007.edenclient.utils.PlayerUtils.sendModMessage;

public class WordHighlighter {

    private List<String> words = new ArrayList<>();
    private boolean enabled;

    private Style style = Style.EMPTY;

    public WordHighlighter() {
        registerCommand("highlight");
        registerCommand("hl");
        AddChatMessageCallback.EVENT.register(this::onChat);
        ConfigSaveCallback.EVENT.register(this::onSave);
        ConfigLoadCallback.EVENT.register(this::onLoad);
    }

    private void onChat(AddChatMessageCallback.ChatAddEvent event) {
        if (!enabled || event.getChatText() == null) return;
        for (String word : words) {
            event.setChatText(highlight(event.getChatText(), word));
        }
    }

    private void onLoad(NbtCompound compoundTag) {
        NbtCompound tag = compoundTag.getCompound("wordhighlighter");
        style = Style.EMPTY.withFormatting(Formatting.AQUA, Formatting.BOLD);
        if (tag == null) {
            words = new ArrayList<>();
            return;
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
    }

    private void onSave(NbtCompound compoundTag) {
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

        if (style.getColor() != null)
            tag.putInt("color", style.getColor().getRgb());
        else
            tag.putInt("color", 16755200);

        compoundTag.put("wordhighlighter", tag);
    }

    private void registerCommand(String cmd) {
        LiteralArgumentBuilder<ClientCommandSource> node = literal(cmd);
        node.then(literal("toggle").executes(c -> {
            enabled = !enabled;
            sendModMessage(new LiteralText(enabled ? "Enabled WordHighlighter!" : "Disabled WordHighlighter!").formatted(Formatting.GOLD));
            return 0;
        }));
        node.then(literal("add").then(argument("word", StringArgumentType.word()).executes(c -> {
            addWord(c.getArgument("word", String.class));
            return 0;
        })));
        node.then(literal("remove").then(argument("word", StringArgumentType.word()).executes(c -> {
            removeWord(c.getArgument("word", String.class));
            return 0;
        })));
        node.then(literal("style").then(argument("style", StringArgumentType.word()).executes(c -> {
            setStyle(c.getArgument("style", String.class));
            return 0;
        })));
        node.then(literal("color").then(
                argument("r", IntegerArgumentType.integer(0, 255)).then(
                        argument("g", IntegerArgumentType.integer(0, 255)).then(
                                argument("b", IntegerArgumentType.integer(0, 255)).executes(c -> {
                                    setColor(c.getArgument("r", Integer.class),
                                            c.getArgument("g", Integer.class),
                                            c.getArgument("b", Integer.class));
                                    return 0;
                                })))));
        node.then(literal("list").executes(c -> {
            listWords();
            return 0;
        }));
        node.then(literal("clear").executes(c -> {
            clearWords();
            return 0;
        }));
        node.then(literal("bold").executes(c -> {
            setBold(!style.isBold());
            return 0;
        }));
        node.then(literal("italic").executes(c -> {
            setItalic(!style.isItalic());
            return 0;
        }));
        node.then(literal("underline").executes(c -> {
            setUnderlined(!style.isUnderlined());
            return 0;
        }));
        node.executes(c -> {
            sendDebugMessage();
            return 0;
        });
        register(node);
    }

    private void setBold(boolean bold) {
        style = style.withBold(bold);
        sendModMessage(new LiteralText(bold ? "Words are now bold!" : "Words are no longer bold!").formatted(Formatting.GOLD));
    }

    public void setItalic(boolean italic) {
        style = style.withItalic(italic);
        sendModMessage(new LiteralText(italic ? "Words are now italic!" : "Words are no longer italic!").formatted(Formatting.GOLD));
    }

    public void setUnderlined(boolean underlined) {
        style = style.withUnderline(underlined);
        sendModMessage(new LiteralText(underlined ? "Words are now underlined!" : "Words are no longer underlined!").formatted(Formatting.GOLD));
    }

    private void setColor(int r, int g, int b) {
        style = style.withColor(new Color(r, g, b).getRGB());
        sendModMessage(new LiteralText("New color set from RGB values!").formatted(Formatting.GOLD));
    }

    private void setStyle(String s) {
        s = s.toLowerCase();
        if (s.equals("reset")) {
            style = Style.EMPTY.withFormatting(Formatting.AQUA, Formatting.BOLD);
            sendModMessage(new LiteralText("Style reset!").formatted(Formatting.GOLD));
            return;
        }
        style = getStyleFromFormattingCode(s);
        sendModMessage(new LiteralText("Style set from FormattingCodes!").formatted(Formatting.GOLD));
    }

    private Style getStyleFromFormattingCode(String input) {
        Style style = Style.EMPTY;
        style = style.withFormatting(input.chars().mapToObj(c -> (char) c).
                map(Formatting::byCode).filter(Objects::nonNull).toList().toArray(new Formatting[0]));
        return style;
    }

    private void listWords() {
        sendModMessage(new LiteralText("These words are currently highlighted:").formatted(Formatting.GOLD));
        sendModMessage(new LiteralText(words.toString()).formatted(Formatting.GOLD));
    }

    private void clearWords() {
        words.clear();
        sendModMessage(new LiteralText("Cleared all words!").formatted(Formatting.GOLD));
    }

    private void addWord(String word) {
        word = word.toLowerCase();
        if (words.contains(word)) {
            sendModMessage(new LiteralText("Word is already highlighted!").formatted(Formatting.GOLD));
            return;
        }
        sendModMessage(new LiteralText("Added words!").formatted(Formatting.GOLD));
        words.add(word);
        words.sort(Comparator.comparingInt(String::length).reversed());
    }

    private void removeWord(String input) {
        if (words.remove(input))
            sendModMessage(new LiteralText("Removed words").formatted(Formatting.GOLD));
        else
            sendModMessage(new LiteralText("Word was not highlighted").formatted(Formatting.GOLD));
    }

    private void sendUsageDebugMessage() {
        sendModMessage(new LiteralText("Command usage:").formatted(Formatting.GOLD));
        sendModMessage(new LiteralText("/hl [add,remove,toggle,clear,list,bold,italic,underline,style,color]").formatted(Formatting.GOLD));
    }

    private void sendDebugMessage() {
        sendModMessage(new LiteralText("Wrong use of command!").formatted(Formatting.GOLD));
        sendUsageDebugMessage();
    }

    private Text highlight(Text text, String string) {
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
                subtext.add(getStyled(match, style));//replace with rainbow
                s = s.substring(end);
                matcher = pattern.matcher(s);
            }
            if (subtext.isEmpty()) {
                text.getSiblings().replaceAll(x -> highlight(x, string));
                return text;
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
            text.getSiblings().replaceAll(y -> highlight(y, string));
            return text;
        }
    }

    private MutableText getStyled(String string, Style style) {
        if (style.isObfuscated()) {
            final Style finalStyle = style.obfuscated(false);
            MutableText text = new LiteralText("");
            AtomicInteger i = new AtomicInteger();
            string.chars()
                    .mapToObj(c -> new String(new int[]{c}, 0, 1))
                    .map(LiteralText::new)
                    .map(t -> t.setStyle(finalStyle.withColor(getFancyRainbowColorAtIndex(i.getAndIncrement()))))
                    .forEach(text::append);
            return text;
        }
        return new LiteralText(string).setStyle(style);
    }


    private int getFancyRainbowColorAtIndex(int index) {
        final double freq = 0.3;

        int r = (int) (Math.sin(freq * index) * 127 + 128);
        int g = (int) (Math.sin(freq * index + 2) * 127 + 128);
        int b = (int) (Math.sin(freq * index + 4) * 127 + 128);
        return new Color(r, g, b).getRGB();
    }
}
