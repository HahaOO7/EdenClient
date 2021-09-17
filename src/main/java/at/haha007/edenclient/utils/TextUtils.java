package at.haha007.edenclient.utils;

import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class TextUtils {
    public static LiteralText createGoldText(String input) {
        return (LiteralText) new LiteralText(input).formatted(Formatting.GOLD);
    }

    public static LiteralText createAquaText(String input) {
        return (LiteralText) new LiteralText(input).formatted(Formatting.AQUA);
    }
}
