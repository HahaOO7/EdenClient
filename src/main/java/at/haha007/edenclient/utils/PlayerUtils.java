package at.haha007.edenclient.utils;

import at.haha007.edenclient.mixinterface.IHandledScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class PlayerUtils {

    private static final Text prefix = new LiteralText("[EC] ").setStyle(Style.EMPTY.withFormatting(Formatting.LIGHT_PURPLE, Formatting.BOLD));

    public static void messageC2S(String msg) {
        ClientPlayerEntity player =PlayerUtils.getPlayer();
        if (player == null || msg.length() > 256) return;
        player.sendChatMessage(msg);
    }

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

    public static void clickSlot(int slotId) {
        Screen screen = MinecraftClient.getInstance().currentScreen;
        if(!(screen instanceof GenericContainerScreen gcs))return;
        ((IHandledScreen) screen).clickMouse(gcs.getScreenHandler().slots.get(slotId), slotId, 0, SlotActionType.PICKUP_ALL);
    }

    public static ClientPlayerEntity getPlayer() {
        return MinecraftClient.getInstance().player;
    }
}
