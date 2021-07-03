package at.haha007.edenclient.utils;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class PlayerUtils {

    private static final Text prefix = new LiteralText("[EC] ").setStyle(Style.EMPTY.withBold(true).withFormatting(Formatting.LIGHT_PURPLE));

    public static void sendMessage(Text text) {
        MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(text);
        System.out.println(text.getString());
    }

    public static void sendModMessage(Text text) {
        sendMessage(prefix.copy().append(text));
    }
}
