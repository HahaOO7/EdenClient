package at.haha007.edenclient.utils;

import net.minecraft.ChatFormatting;

import java.awt.*;
import java.util.List;

public class ColorUtils {
    private static final List<Character> simpleRainbowColors = List.of('c', '6', 'e', 'a', 'b', '3', 'd', '5', 'd', '3', 'b', 'a', 'e', '6');

    public static String colorToPrefix(Color color) {
        return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
    }

    public static String colorToLegacyPrefix(Color color) {
        ChatFormatting closestFormatting = ChatFormatting.WHITE;
        double closestDistance = Double.MAX_VALUE;
        for (ChatFormatting value : ChatFormatting.values()) {
            Integer colorCode = value.getColor();
            if (colorCode == null) {
                continue;
            }
            Color c1 = new Color(colorCode);
            double distance = getDifference(color, c1);
            if (distance > closestDistance) {
                continue;
            }
            closestDistance = distance;
            closestFormatting = value;
        }
        return closestFormatting.getChar() + "";
    }

    private static double getDifference(Color color1, Color color2) {
        int rDiff = color1.getRed() - color2.getRed();
        int gDiff = color1.getGreen() - color2.getGreen();
        int bDiff = color1.getBlue() - color2.getBlue();
        return Math.sqrt(rDiff * rDiff + gDiff * gDiff + bDiff * bDiff);
    }
}
