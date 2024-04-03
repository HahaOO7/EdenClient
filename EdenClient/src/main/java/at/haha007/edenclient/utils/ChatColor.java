package at.haha007.edenclient.utils;

import java.awt.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.DataResult;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

public class ChatColor {

    public static final String BLACK = ChatFormatting.BLACK.toString();
    public static final String DARK_BLUE = ChatFormatting.DARK_BLUE.toString();
    public static final String DARK_GREEN = ChatFormatting.DARK_GREEN.toString();
    public static final String DARK_AQUA = ChatFormatting.DARK_AQUA.toString();
    public static final String DARK_RED = ChatFormatting.DARK_RED.toString();
    public static final String DARK_PURPLE = ChatFormatting.DARK_PURPLE.toString();
    public static final String GOLD = ChatFormatting.GOLD.toString();
    public static final String GRAY = ChatFormatting.GRAY.toString();
    public static final String DARK_GRAY = ChatFormatting.DARK_GRAY.toString();
    public static final String BLUE = ChatFormatting.BLUE.toString();
    public static final String GREEN = ChatFormatting.GREEN.toString();
    public static final String AQUA = ChatFormatting.AQUA.toString();
    public static final String RED = ChatFormatting.RED.toString();
    public static final String LIGHT_PURPLE = ChatFormatting.LIGHT_PURPLE.toString();
    public static final String YELLOW = ChatFormatting.YELLOW.toString();
    public static final String WHITE = ChatFormatting.WHITE.toString();
    public static final String OBFUSCATED = ChatFormatting.OBFUSCATED.toString();
    public static final String BOLD = ChatFormatting.BOLD.toString();
    public static final String STRIKETHROUGH = ChatFormatting.STRIKETHROUGH.toString();
    public static final String UNDERLINE = ChatFormatting.UNDERLINE.toString();
    public static final String ITALIC = ChatFormatting.ITALIC.toString();
    public static final String RESET = ChatFormatting.RESET.toString();


    private final byte r, g, b;

    public ChatColor(Color color) {
        this.r = (byte) color.getRed();
        this.g = (byte) color.getGreen();
        this.b = (byte) color.getBlue();
    }

    public ChatColor(int r, int g, int b) {
        this.r = (byte) r;
        this.g = (byte) g;
        this.b = (byte) b;
    }

    public String toString() {
        return String.format("§#%02X%02X%02X", r, g, b);
    }

    public static MutableComponent translateColors(String string) {
        final Pattern hex = Pattern.compile("§#[0-9a-fA-F]{6}");
        MutableComponent text = Component.literal("");
        Style style = Style.EMPTY;
        Matcher matcher = hex.matcher(string);
        while (matcher.find()) {
            int start = matcher.start();
            int end = matcher.end();
            text.append(Component.literal(string.substring(0, start)).setStyle(style));
            Either<TextColor, DataResult.PartialResult<TextColor>> result = TextColor.parseColor(string.substring(start + 1, end)).get();
            if (result.left().isPresent()) {
                style = style.withColor(result.left().get());
            }
            string = string.substring(end);
            matcher = hex.matcher(string);
        }
        text.append(Component.literal(string).setStyle(style));
        return text;
    }
}
