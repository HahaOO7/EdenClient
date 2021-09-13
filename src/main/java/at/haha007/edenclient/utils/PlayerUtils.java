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
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class PlayerUtils {

    private static final Text prefix = new LiteralText("[EC] ").setStyle(Style.EMPTY.withFormatting(Formatting.LIGHT_PURPLE, Formatting.BOLD));

    public static void messageC2S(String msg) {
        ClientPlayerEntity player = PlayerUtils.getPlayer();
        if (msg.length() > 256) {
            sendModMessage("Tried sending message longer than 256 characters: " + msg);
            return;
        }
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
        if (!(screen instanceof GenericContainerScreen gcs)) return;
        ((IHandledScreen) screen).clickMouse(gcs.getScreenHandler().slots.get(slotId), slotId, 0, SlotActionType.PICKUP_ALL);
    }

    public static Vec3d getClientLookVec() {
        ClientPlayerEntity player = getPlayer();
        float f = 0.017453292F;
        float pi = (float) Math.PI;

        float f1 = MathHelper.cos(-player.getYaw() * f - pi);
        float f2 = MathHelper.sin(-player.getYaw() * f - pi);
        float f3 = -MathHelper.cos(-player.getPitch() * f);
        float f4 = MathHelper.sin(-player.getPitch() * f);

        return new Vec3d(f2 * f3, f4, f1 * f3);
    }

    public static ClientPlayerEntity getPlayer() {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null)
            throw new NullPointerException("Player is null.");
        return player;
    }
}
