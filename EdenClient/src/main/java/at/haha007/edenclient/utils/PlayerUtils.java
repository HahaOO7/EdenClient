package at.haha007.edenclient.utils;

import at.haha007.edenclient.mixinterface.IHandledScreen;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Optional;

public class PlayerUtils {

    private static final Component prefix = Component.literal("[EC] ").setStyle(Style.EMPTY.applyFormats(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD));
    private static final net.kyori.adventure.text.Component adventurePrefix = net.kyori.adventure.text.Component.text("[EC] ", NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD);

    public static void messageC2S(String msg) {
        LocalPlayer player = PlayerUtils.getPlayer();
        if (msg.length() > 256) {
            sendModMessage("Tried sending message longer than 256 characters: " + msg);
            return;
        }
        if (msg.startsWith("/"))
            player.connection.sendCommand(msg.substring(1));
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

    public static void sendActionBar(net.kyori.adventure.text.Component text) {
        String json = GsonComponentSerializer.gson().serialize(text);
        MutableComponent component = Component.Serializer.fromJson(json, RegistryAccess.EMPTY);
        Minecraft.getInstance().gui.setOverlayMessage(component, true);
    }

    public static void sendModMessage(net.kyori.adventure.text.Component text) {
        String json = GsonComponentSerializer.gson().serialize(text);
        MutableComponent component = Component.Serializer.fromJson(json, RegistryAccess.EMPTY);
        Minecraft.getInstance().gui.getChat().addMessage(component);
    }

    public static void sendModMessage(String text) {
        sendModMessage(net.kyori.adventure.text.Component.text(text, NamedTextColor.GOLD));
    }

    public static Component createModMessage(String text) {
        return createModMessage(Component.literal(text));
    }

    @Deprecated
    public static Component createModMessage(Component text) {
        return Component.empty().append(prefix).append(Component.empty().append(text).withStyle(ChatFormatting.GOLD));
    }

    public static void walkTowards(Vec3 target) {
        var player = getPlayer();
        //get delta vec
        Vec3 vec = target.subtract(player.position());
        //remove vertical component
        vec = vec.subtract(0, vec.y, 0);
        //if the horizontal component is less than 0.1 we have reached the destination
        if (vec.length() <= 0.1) return;

        //calculate the movement speed
        double genericMovementSpeed = player.getSpeed();
        double speed = Optional.ofNullable(player.getEffect(MobEffects.MOVEMENT_SPEED)).map(MobEffectInstance::getAmplifier).orElse(-1) + 1;
        double slow = Optional.ofNullable(player.getEffect(MobEffects.MOVEMENT_SLOWDOWN)).map(MobEffectInstance::getAmplifier).orElse(-1) + 1;
        //this formula is not exact, but close enough
        double movementSpeed = genericMovementSpeed * 10 * (5.612 + speed * 1.123 - slow * 0.841);
        movementSpeed /= 20;

        //scale the vector to the movement speed
        movementSpeed = Math.min(target.distanceTo(player.position()) * .5, movementSpeed);
        vec = vec.normalize().scale(movementSpeed);


        //move the player
        player.move(MoverType.SELF, vec);

        player.position().distanceTo(target);
    }

    public static void walkTowards(Vec3i target) {
        Vec3 t = Vec3.atBottomCenterOf(target);
        walkTowards(t);
    }

    public static void clickSlot(int slotId) {
        Screen screen = Minecraft.getInstance().screen;
        if (!(screen instanceof ContainerScreen gcs)) return;
        ((IHandledScreen) screen).edenClient$clickMouse(gcs.getMenu().slots.get(slotId), slotId, 0, ClickType.PICKUP_ALL);
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

    /**
     * Attack a block
     *
     * @param pos The position to break
     * @return if the block was broken
     */
    public static boolean breakBlock(BlockPos pos) {
        LocalPlayer player = getPlayer();
        ClientLevel world = player.clientLevel;
        BlockState state = world.getBlockState(pos);
        Block block = state.getBlock();
        float delta = state.getDestroyProgress(player, world, pos);
        ClientPacketListener nh = player.connection;
        boolean instantMine = delta >= 1;
        Direction dir = getHitDirectionForBlock(player, pos);
        if (instantMine) {
            nh.send(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, pos, dir));
            world.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            return true;
        }
        MultiPlayerGameMode gameMode = Minecraft.getInstance().gameMode;
        if (gameMode == null) return false;
        gameMode.continueDestroyBlock(pos, dir);
        state = world.getBlockState(pos);
        return state.getBlock() != block;
    }

    public static boolean selectPlacableBlock() {
        LocalPlayer player = getPlayer();
        Inventory inventory = player.getInventory();

        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (connection == null) return false;
        MultiPlayerGameMode gameMode = Minecraft.getInstance().gameMode;
        if (gameMode == null) return false;
        ClientLevel level = player.clientLevel;

        int slot = -1;
        for (int i = 0; i < 36; i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.isEmpty()) continue;
            Item item = stack.getItem();
            if (!(item instanceof BlockItem blockItem)) continue;
            Block block = blockItem.getBlock();
            BlockState defaultState = block.defaultBlockState();
            if (!defaultState.isCollisionShapeFullBlock(level, BlockPos.ZERO))
                continue;
            slot = i;
            break;
        }
        if (slot < 0) return false;

        if (slot < 9 && inventory.selected == slot)
            return true;

        if (slot < 9) {
            inventory.selected = slot;
            connection.send(new ServerboundSetCarriedItemPacket(slot));
            return true;
        }

        //replace slot 9
        inventory.selected = 8;
        connection.send(new ServerboundSetCarriedItemPacket(8));
        gameMode.handlePickItem(slot);
        return true;
    }

    public static boolean selectItem(Item item) {
        LocalPlayer player = getPlayer();
        Inventory inventory = player.getInventory();

        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (connection == null) return false;
        MultiPlayerGameMode gameMode = Minecraft.getInstance().gameMode;
        if (gameMode == null) return false;

        int slot = -1;
        for (int i = 0; i < 36; i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.isEmpty()) continue;
            if (!item.equals(inventory.getItem(i).getItem()))
                continue;
            slot = i;
            break;
        }
        if (slot < 0) return false;

        if (slot < 9 && inventory.selected == slot)
            return true;

        if (slot < 9) {
            inventory.selected = slot;
            connection.send(new ServerboundSetCarriedItemPacket(slot));
            return true;
        }

        //select slot 9
        inventory.selected = 8;
        connection.send(new ServerboundSetCarriedItemPacket(8));
        gameMode.handlePickItem(slot);
        return true;
    }

    public static Optional<Item> selectAnyItem(Collection<Item> options) {
        LocalPlayer player = getPlayer();
        Inventory inventory = player.getInventory();

        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (connection == null) return Optional.empty();
        MultiPlayerGameMode gameMode = Minecraft.getInstance().gameMode;
        if (gameMode == null) return Optional.empty();

        int slot = -1;
        Item select = null;
        for (int i = 0; i < 36; i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.isEmpty()) continue;
            Item item = stack.getItem();
            if (!options.contains(item))
                continue;
            slot = i;
            select = item;
            break;
        }
        if (slot < 0) return Optional.empty();

        if (slot < 9 && inventory.selected == slot)
            return Optional.of(select);

        if (slot < 9) {
            inventory.selected = slot;
            connection.send(new ServerboundSetCarriedItemPacket(slot));
            return Optional.of(select);
        }

        //replace slot 9
        inventory.selected = 8;
        connection.send(new ServerboundSetCarriedItemPacket(8));
        gameMode.handlePickItem(slot);
        return Optional.of(select);
    }
}
