package at.haha007.edenclient.mods;

import at.haha007.edenclient.annotations.Mod;
import at.haha007.edenclient.callbacks.AddChatMessageCallback;
import at.haha007.edenclient.utils.config.ConfigSubscriber;
import at.haha007.edenclient.utils.config.PerWorldConfig;
import at.haha007.edenclient.utils.config.wrappers.StringList;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.contents.PlainTextContents;
import net.minecraft.network.chat.contents.TranslatableContents;

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

@Mod
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
        AddChatMessageCallback.EVENT.register(this::onChat, getClass());
        PerWorldConfig.get().register(this, "wordhighlighter");
    }

    private void onChat(AddChatMessageCallback.ChatAddEvent event) {
        if (!enabled || event.getChatText() == null) return;
        for (String word : words) {
            event.setChatText(highlight(event.getChatText().copy(), word));
        }
    }

    private void registerCommand(String name) {
        LiteralArgumentBuilder<ClientSuggestionProvider> node = literal(name);
        node.then(literal("toggle").executes(c -> {
            enabled = !enabled;
            sendModMessage(enabled ? "Enabled WordHighlighter!" : "Disabled WordHighlighter!");
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
        sendModMessage(bold ? "Words are now bold!" : "Words are no longer bold!");
    }

    public void setItalic(boolean italic) {
        style = style.withItalic(italic);
        sendModMessage(italic ? "Words are now italic!" : "Words are no longer italic!");
    }

    public void setUnderlined(boolean underlined) {
        style = style.withUnderlined(underlined);
        sendModMessage(underlined ? "Words are now underlined!" : "Words are no longer underlined!");
    }

    private void setColor(int r, int g, int b) {
        style = style.withColor(new Color(r, g, b).getRGB());
        sendModMessage("New color set from RGB values!");
    }

    private void setStyle(String s) {
        s = s.toLowerCase();
        if (s.equals("reset")) {
            style = Style.EMPTY.applyFormats(ChatFormatting.AQUA, ChatFormatting.BOLD);
            sendModMessage("Style reset!");
            return;
        }
        style = getStyleFromFormattingCode(s);
        sendModMessage("Style set from FormattingCodes!");
    }

    private Style getStyleFromFormattingCode(String input) {
        return Style.EMPTY.applyFormats(input.chars().mapToObj(c -> (char) c).
                map(ChatFormatting::getByCode).filter(Objects::nonNull).toList().toArray(new ChatFormatting[0]));
    }

    private void listWords() {
        sendModMessage("These words are currently highlighted:");
        sendModMessage(words.toString());
    }

    private void clearWords() {
        words.clear();
        sendModMessage("Cleared all words!");
    }

    private void addWord(String word) {
        word = word.toLowerCase();
        if (words.contains(word)) {
            sendModMessage("Word is already highlighted!");
            return;
        }
        sendModMessage("Added words!");
        words.add(word);
        words.sort(Comparator.comparingInt(String::length).reversed());
    }

    private void removeWord(String input) {
        if (words.remove(input))
            sendModMessage("Removed words");
        else
            sendModMessage("Word was not highlighted");
    }

    private void sendUsageDebugMessage() {
        sendModMessage("Command usage:");
        sendModMessage("/ehl [add,remove,toggle,clear,list,bold,italic,underline,style,color]");
    }

    private void sendDebugMessage() {
        sendModMessage("Wrong use of command!");
        sendUsageDebugMessage();
    }

    private MutableComponent highlight(MutableComponent txt, String filter) {
        if (txt.getContents() instanceof PlainTextContents.LiteralContents t) {
            String s = t.text();
            List<MutableComponent> subtext = new ArrayList<>();
            Style baseStyle = txt.getStyle();
            s = applyMatcher(filter, s, subtext, baseStyle);
            if (subtext.isEmpty()) {
                txt.getSiblings().replaceAll(x -> highlight(x.copy(), filter));
                return txt;
            }
            if (!s.isEmpty())
                subtext.add(Component.literal(s).setStyle(baseStyle));
            MutableComponent nextText = Component.literal("");
            subtext.forEach(nextText::append);
            txt.getSiblings().stream().map(sibling -> highlight(sibling.copy(), filter)).forEach(nextText::append);
            return nextText;
        } else if (txt.getContents() instanceof TranslatableContents t) {
            Object[] args = t.getArgs();
            for (int i = 0; i < args.length; i++) {
                if (args[i] instanceof Component y)
                    args[i] = highlight(y.copy(), filter);
                else if (args[i] instanceof String y)
                    args[i] = highlight(Component.literal(y), filter);
            }
            txt.getSiblings().replaceAll(y -> highlight(y.copy(), filter));
            return txt;
        } else {
            txt.getSiblings().replaceAll(y -> highlight(y.copy(), filter));
            return txt;
        }
    }

    private String applyMatcher(String string, String s, List<MutableComponent> subtext, Style baseStyle) {
        Pattern pattern = Pattern.compile(string, Pattern.CASE_INSENSITIVE | Pattern.LITERAL);
        Matcher matcher = pattern.matcher(s);
        Style backupStyle = this.style.withHoverEvent(baseStyle.getHoverEvent()).withClickEvent(baseStyle.getClickEvent());
        while (matcher.find()) {
            int start = matcher.start();
            int end = matcher.end();
            String pre = s.substring(0, start);
            String match = s.substring(start, end);
            if (!pre.isEmpty())
                subtext.add(Component.literal(pre).setStyle(baseStyle));
            subtext.add(getStyled(match, backupStyle));//replace with rainbow
            s = s.substring(end);
            matcher = pattern.matcher(s);
        }
        return s;
    }

    private MutableComponent getStyled(String string, Style style) {
        if (style.isObfuscated()) {
            final Style finalStyle = style.withObfuscated(false);
            MutableComponent text = Component.literal("");
            AtomicInteger i = new AtomicInteger();
            string.chars()
                    .mapToObj(c -> new String(new int[]{c}, 0, 1))
                    .map(Component::literal)
                    .map(t -> t.setStyle(finalStyle.withColor(getFancyRainbowColorAtIndex(i.getAndIncrement()))))
                    .forEach(text::append);
            return text;
        }
        return Component.literal(string).setStyle(style);
    }


    private int getFancyRainbowColorAtIndex(int index) {
        final double freq = 0.6;

        int r = (int) (Math.sin(freq * index) * 127 + 128);
        int g = (int) (Math.sin(freq * index + 2) * 127 + 128);
        int b = (int) (Math.sin(freq * index + 4) * 127 + 128);
        return new Color(r, g, b).getRGB();
    }
}
