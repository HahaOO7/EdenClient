package at.haha007.edenclient.utils;

import net.minecraft.text.LiteralText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;

import java.awt.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatColor {

    public static final String BLACK = Formatting.BLACK.toString();
    public static final String DARK_BLUE = Formatting.DARK_BLUE.toString();
    public static final String DARK_GREEN = Formatting.DARK_GREEN.toString();
    public static final String DARK_AQUA = Formatting.DARK_AQUA.toString();
    public static final String DARK_RED = Formatting.DARK_RED.toString();
    public static final String DARK_PURPLE = Formatting.DARK_PURPLE.toString();
    public static final String GOLD = Formatting.GOLD.toString();
    public static final String GRAY = Formatting.GRAY.toString();
    public static final String DARK_GRAY = Formatting.DARK_GRAY.toString();
    public static final String BLUE = Formatting.BLUE.toString();
    public static final String GREEN = Formatting.GREEN.toString();
    public static final String AQUA = Formatting.AQUA.toString();
    public static final String RED = Formatting.RED.toString();
    public static final String LIGHT_PURPLE = Formatting.LIGHT_PURPLE.toString();
    public static final String YELLOW = Formatting.YELLOW.toString();
    public static final String WHITE = Formatting.WHITE.toString();
    public static final String OBFUSCATED = Formatting.OBFUSCATED.toString();
    public static final String BOLD = Formatting.BOLD.toString();
    public static final String STRIKETHROUGH = Formatting.STRIKETHROUGH.toString();
    public static final String UNDERLINE = Formatting.UNDERLINE.toString();
    public static final String ITALIC = Formatting.ITALIC.toString();
    public static final String RESET = Formatting.RESET.toString();


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

    public static Text translateColors(String string) {
        final Pattern hex = Pattern.compile("§#[0-9a-fA-F]{6}");
        LiteralText text = new LiteralText("");
        Style style = Style.EMPTY;
        Matcher matcher = hex.matcher(string);
        while (matcher.find()) {
            int start = matcher.start();
            int end = matcher.end();
            text.append(new LiteralText(string.substring(0, start)).setStyle(style));
            style = style.withColor(TextColor.parse(string.substring(start + 1, end)));
            string = string.substring(end);
            matcher = hex.matcher(string);
        }
        text.append(new LiteralText(string).setStyle(style));
        return text;
    }
}