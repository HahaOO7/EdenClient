package at.haha007.edenclient.utils;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class PlayerUtils {

    private static final Text prefix = new LiteralText("[EC] ").setStyle(Style.EMPTY.withFormatting(Formatting.LIGHT_PURPLE, Formatting.BOLD));

    public static void sendMessage(Text text) {
        MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(text);
        System.out.println(text.getString());
    }

    public static void sendTitle(Text title, Text subtitle, int in, int keep, int out) {
        MinecraftClient.getInstance().inGameHud.setSubtitle(subtitle);
        MinecraftClient.getInstance().inGameHud.setTitle(title);
        MinecraftClient.getInstance().inGameHud.setTitleTicks(in, keep, out);
    }

    public static void sendActionBar(Text text) {
        MinecraftClient.getInstance().inGameHud.setOverlayMessage(text, false);
        System.out.println(text.getString());
    }

    public static void sendModMessage(Text text) {
        sendMessage(prefix.copy().append(text));
    }

    public static void sendModMessage(String text) {
        sendModMessage(new LiteralText(text).formatted(Formatting.GOLD));
    }
}
