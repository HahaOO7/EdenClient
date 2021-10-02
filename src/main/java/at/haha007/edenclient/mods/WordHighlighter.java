package at.haha007.edenclient.mods;

import at.haha007.edenclient.callbacks.AddChatMessageCallback;
import at.haha007.edenclient.utils.ChatColor;
import at.haha007.edenclient.utils.config.ConfigSubscriber;
import at.haha007.edenclient.utils.config.PerWorldConfig;
import at.haha007.edenclient.utils.config.wrappers.StringList;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.client.network.ClientCommandSource;
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
    @ConfigSubscriber
    private final StringList words = new StringList();
    @ConfigSubscriber("false")
    private boolean enabled;

    @ConfigSubscriber
    private Style style = Style.EMPTY;

    public WordHighlighter() {
        registerCommand("ehighlight");
        registerCommand("ehl");
        AddChatMessageCallback.EVENT.register(this::onChat);
        PerWorldConfig.get().register(this, "wordhighlighter");
    }

    private void onChat(AddChatMessageCallback.ChatAddEvent event) {
        if (!enabled || event.getChatText() == null) return;
        for (String word : words) {
            event.setChatText(highlight(event.getChatText(), word));
        }
    }

    private void registerCommand(String name) {
        LiteralArgumentBuilder<ClientCommandSource> node = literal(name);
        node.then(literal("toggle").executes(c -> {
            enabled = !enabled;
            sendModMessage(ChatColor.GOLD + (enabled ? "Enabled WordHighlighter!" : "Disabled WordHighlighter!"));
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

        register(node,
                "WordHighlighter allows you to highlight specific words when they appear in chat.",
                "You are also able to set a specific style in which they should be displayed.");
    }

    private void setBold(boolean bold) {
        style = style.withBold(bold);
        sendModMessage(ChatColor.GOLD + (bold ? "Words are now bold!" : "Words are no longer bold!"));
    }

    public void setItalic(boolean italic) {
        style = style.withItalic(italic);
        sendModMessage(ChatColor.GOLD + (italic ? "Words are now italic!" : "Words are no longer italic!"));
    }

    public void setUnderlined(boolean underlined) {
        style = style.withUnderline(underlined);
        sendModMessage(ChatColor.GOLD + (underlined ? "Words are now underlined!" : "Words are no longer underlined!"));
    }

    private void setColor(int r, int g, int b) {
        style = style.withColor(new Color(r, g, b).getRGB());
        sendModMessage(ChatColor.GOLD + "New color set from RGB values!");
    }

    private void setStyle(String s) {
        s = s.toLowerCase();
        if (s.equals("reset")) {
            style = Style.EMPTY.withFormatting(Formatting.AQUA, Formatting.BOLD);
            sendModMessage(ChatColor.GOLD + "Style reset!");
            return;
        }
        style = getStyleFromFormattingCode(s);
        sendModMessage(ChatColor.GOLD + "Style set from FormattingCodes!");
    }

    private Style getStyleFromFormattingCode(String input) {
        Style style = Style.EMPTY;
        style = style.withFormatting(input.chars().mapToObj(c -> (char) c).
                map(Formatting::byCode).filter(Objects::nonNull).toList().toArray(new Formatting[0]));
        return style;
    }

    private void listWords() {
        sendModMessage(ChatColor.GOLD + "These words are currently highlighted:");
        sendModMessage(ChatColor.GOLD + words);
    }

    private void clearWords() {
        words.clear();
        sendModMessage(ChatColor.GOLD + "Cleared all words!");
    }

    private void addWord(String word) {
        word = word.toLowerCase();
        if (words.contains(word)) {
            sendModMessage(ChatColor.GOLD + "Word is already highlighted!");
            return;
        }
        sendModMessage(ChatColor.GOLD + "Added words!");
        words.add(word);
        words.sort(Comparator.comparingInt(String::length).reversed());
    }

    private void removeWord(String input) {
        if (words.remove(input))
            sendModMessage(ChatColor.GOLD + "Removed words");
        else
            sendModMessage(ChatColor.GOLD + "Word was not highlighted");
    }

    private void sendUsageDebugMessage() {
        sendModMessage(ChatColor.GOLD + "Command usage:");
        sendModMessage(ChatColor.GOLD + "/hl [add,remove,toggle,clear,list,bold,italic,underline,style,color]");
    }

    private void sendDebugMessage() {
        sendModMessage(ChatColor.GOLD + "Wrong use of command!");
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
        final double freq = 0.6;

        int r = (int) (Math.sin(freq * index) * 127 + 128);
        int g = (int) (Math.sin(freq * index + 2) * 127 + 128);
        int b = (int) (Math.sin(freq * index + 4) * 127 + 128);
        return new Color(r, g, b).getRGB();
    }
}
