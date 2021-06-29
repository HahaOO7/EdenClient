package at.haha007.edenclient.mods.wordhighlighter;

import at.haha007.edenclient.utils.PlayerUtils;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Style;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;

import java.util.Set;

public class WordHighlighterUtils {
    protected static boolean shouldHighlight(String string, Set<String> words) {
        for (String word : words) {
            if (string.contains(word))
                return true;
        }
        return false;
    }

    protected static void sendDebugMessage() {
        PlayerUtils.sendMessage(new LiteralText("[Eden] Wrong use of command!").formatted(Formatting.GOLD));
        PlayerUtils.sendMessage(new LiteralText("[Eden] Use one of the following arguments: [add, remove, list]!").formatted(Formatting.GOLD));
        PlayerUtils.sendMessage(new LiteralText("[Eden] E.G: /highlights add EmielRegis").formatted(Formatting.GOLD));
    }

    protected static String getStyleString(Style style) {
        String s = "§r";

        if (style.isEmpty())
            return "§r";
        if (style.isObfuscated())
            s += "§k";
        if (style.isBold())
            s += "§l";
        if (style.isStrikethrough())
            s += "§m";
        if (style.isUnderlined())
            s += "§n";
        if (style.isItalic())
            s += "§o";

        TextColor color = style.getColor();
        if (color == null) {
            s += "§f";
            return s;
        }

        switch (color.getRgb()) {
            case 0:
                s += "§0";
                break;
            case 170:
                s += "§1";
                break;
            case 43520:
                s += "§2";
                break;
            case 43690:
                s += "§3";
                break;
            case 11141120:
                s += "§4";
                break;
            case 11141290:
                s += "§5";
                break;
            case 16755200:
                s += "§6";
                break;
            case 11184810:
                s += "§7";
                break;
            case 5592405:
                s += "§8";
                break;
            case 5592575:
                s += "§9";
                break;
            case 5635925:
                s += "§a";
                break;
            case 5636095:
                s += "§b";
                break;
            case 16733525:
                s += "§c";
                break;
            case 16733695:
                s += "§d";
                break;
            case 16777045:
                s += "§e";
                break;
            case 16777215:
                s += "§f";
                break;
        }
        return s;
    }

    public static Style getCurrentStyle(Style style, char char1) {
        switch (char1) {
            case 'r':
                style = Style.EMPTY;
                break;
            case 'l':
                style = style.withBold(true);
                break;
            case 'n':
                style = style.withUnderline(true);
                break;
            case 'o':
                style = style.withItalic(true);
                break;
            case 'k':
            case 'm':
                break;
            case '0':
                style = style.withColor(TextColor.fromRgb(0));
                break;
            case '1':
                style = style.withColor(TextColor.fromRgb(170));
                break;
            case '2':
                style = style.withColor(TextColor.fromRgb(43520));
                break;
            case '3':
                style = style.withColor(TextColor.fromRgb(43690));
                break;
            case '4':
                style = style.withColor(TextColor.fromRgb(11141120));
                break;
            case '5':
                style = style.withColor(TextColor.fromRgb(11141290));
                break;
            case '6':
                style = style.withColor(TextColor.fromRgb(16755200));
                break;
            case '7':
                style = style.withColor(TextColor.fromRgb(11184810));
                break;
            case '8':
                style = style.withColor(TextColor.fromRgb(5592405));
                break;
            case '9':
                style = style.withColor(TextColor.fromRgb(5592575));
                break;
            case 'a':
                style = style.withColor(TextColor.fromRgb(5635925));
                break;
            case 'b':
                style = style.withColor(TextColor.fromRgb(5636095));
                break;
            case 'c':
                style = style.withColor(TextColor.fromRgb(16733525));
                break;
            case 'd':
                style = style.withColor(TextColor.fromRgb(16733695));
                break;
            case 'e':
                style = style.withColor(TextColor.fromRgb(16777045));
                break;
            case 'f':
                style = style.withColor(TextColor.fromRgb(16777215));
                break;
        }
        return style;
    }
}
