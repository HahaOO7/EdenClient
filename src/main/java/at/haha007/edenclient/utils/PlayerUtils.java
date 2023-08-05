package at.haha007.edenclient.utils;

import at.haha007.edenclient.mixinterface.IHandledScreen;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.phys.Vec3;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Optional;

public class PlayerUtils {

    private static final Component prefix = Component.literal("[EC] ").setStyle(Style.EMPTY.applyFormats(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD));

    public static void messageC2S(String msg) {
        LocalPlayer player = PlayerUtils.getPlayer();
        if (msg.length() > 256) {
            sendModMessage("Tried sending message longer than 256 characters: " + msg);
            return;
        }
        if (msg.startsWith("/"))
            player.connection.sendCommand(msg);
        else
            player.connection.sendChat(msg);
    }

    public static void sendMessage(Component text) {
        Minecraft.getInstance().gui.getChat().addMessage(text);
    }

    public static void sendTitle(Component title, Component subtitle, int in, int keep, int out) {
        Minecraft.getInstance().gui.setSubtitle(subtitle);
        Minecraft.getInstance().gui.setTitle(title);
        Minecraft.getInstance().gui.setTimes(in, keep, out);
    }

    public static void sendActionBar(Component text) {
        Minecraft.getInstance().gui.setOverlayMessage(text, false);
    }

    public static void sendModMessage(Component text) {
        sendMessage(Component.empty().append(prefix).append(Component.empty().append(text).withStyle(ChatFormatting.GOLD)));
    }

    public static void sendModMessage(String text) {
        sendModMessage(Component.literal(text));
    }

    public static void clickSlot(int slotId) {
        Screen screen = Minecraft.getInstance().screen;
        if (!(screen instanceof ContainerScreen gcs)) return;
        ((IHandledScreen) screen).clickMouse(gcs.getMenu().slots.get(slotId), slotId, 0, ClickType.PICKUP_ALL);
    }

    public static Vec3 getClientLookVec() {
        LocalPlayer player = getPlayer();
        float f = 0.017453292F;
        float pi = (float) Math.PI;

        float f1 = Mth.cos(-player.getYRot() * f - pi);
        float f2 = Mth.sin(-player.getYRot() * f - pi);
        float f3 = -Mth.cos(-player.getXRot() * f);
        float f4 = Mth.sin(-player.getXRot() * f);

        return new Vec3(f2 * f3, f4, f1 * f3);
    }

    public static LocalPlayer getPlayer() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null)
            throw new IllegalStateException("Player is null.");
        return player;
    }

    public static Direction getHitDirectionForBlock(LocalPlayer player, BlockPos target) {
        Vec3 playerPos = player.getEyePosition();
        Optional<Direction> direction = Arrays.stream(Direction.values())
                .min(Comparator.comparingDouble(
                        dir -> Vec3.atLowerCornerOf(dir.getNormal()).multiply(.5, .5, .5).add(Vec3.atLowerCornerOf(target)).distanceTo(playerPos)));

        return direction.orElse(null);
    }

    public static ClientPacketListener networkHandler() {
        return getPlayer().connection;
    }
}
