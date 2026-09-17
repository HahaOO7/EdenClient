package at.haha007.edenclient.utils;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.TextColor;

import java.awt.*;
import java.util.List;

public class ColorUtils {

    public static String colorToLegacyPrefix(Color color) {
        ChatFormatting closestFormatting = ChatFormatting.WHITE;
        double closestDistance = Double.MAX_VALUE;
        for (ChatFormatting value : ChatFormatting.values()) {
            TextColor textColor = TextColor.fromLegacyFormat(value);
            if (textColor == null) {
                continue;
            }
            Color c1 = new Color(textColor.getValue());
            double distance = getDifference(color, c1);
            if (distance > closestDistance) {
                continue;
            }
            closestDistance = distance;
            closestFormatting = value;
        }
        return closestFormatting.toString().substring(1);
    }

    private static double getDifference(Color color1, Color color2) {
        int rDiff = color1.getRed() - color2.getRed();
        int gDiff = color1.getGreen() - color2.getGreen();
        int bDiff = color1.getBlue() - color2.getBlue();
        return Math.sqrt(rDiff * rDiff + gDiff * gDiff + bDiff * bDiff);
    }
}
