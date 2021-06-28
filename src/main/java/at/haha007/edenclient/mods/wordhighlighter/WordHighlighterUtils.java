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
            case 11141120:
                s += "§4";
                break;
            case 16733525:
                s += "§c";
                break;
            case 16755200:
                s += "§6";
                break;
            case 16777045:
                s += "§e";
                break;
            case 43520:
                s += "§2";
                break;
            case 5635925:
                s += "§a";
                break;
            case 5636095:
                s += "§b";
                break;
            case 43690:
                s += "§3";
                break;
            case 170:
                s += "§1";
                break;
            case 5592575:
                s += "§9";
                break;
            case 16733695:
                s += "§d";
                break;
            case 11141290:
                s += "§5";
                break;
            case 16777215:
                s += "§f";
                break;
            case 11184810:
                s += "§7";
                break;
            case 5592405:
                s += "§8";
                break;
            case 0:
                s += "§0";
                break;
        }
        return s;
    }
}
