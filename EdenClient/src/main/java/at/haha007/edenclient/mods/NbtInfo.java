package at.haha007.edenclient.mods;

import at.haha007.edenclient.EdenClient;
import at.haha007.edenclient.annotations.Mod;
import at.haha007.edenclient.callbacks.InventoryKeyCallback;
import at.haha007.edenclient.command.CommandManager;
import at.haha007.edenclient.mixinterface.HoveredSlotAccessor;
import at.haha007.edenclient.utils.NbtFormatter;
import at.haha007.edenclient.utils.PlayerUtils;
import at.haha007.edenclient.utils.Scheduler;
import at.haha007.edenclient.utils.config.ConfigSubscriber;
import at.haha007.edenclient.utils.config.PerWorldConfig;
import at.haha007.edenclient.utils.screen.PressKeyScreen;
import at.haha007.edenclient.utils.screen.ShowTextScreen;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import static at.haha007.edenclient.command.CommandManager.literal;
import static at.haha007.edenclient.utils.PlayerUtils.sendModMessage;

@Mod(dependencies = Scheduler.class)
public class NbtInfo {
    @ConfigSubscriber
    private int key = GLFW.GLFW_KEY_ESCAPE;

    private NbtInfo() {
        registerCommand();
        InventoryKeyCallback.EVENT.register(this::onInventoryKey, getClass());
        PerWorldConfig.get().register(this, "NbtInfo");
    }

    private void registerCommand() {
        CommandManager.register(literal("enbt").executes(c -> {
                    LocalPlayer player = PlayerUtils.getPlayer();
                    Inventory inv = player.getInventory();
                    ItemStack stack = inv.getSelectedItem();
                    if (stack.isEmpty()) {
                        sendModMessage("Take an item in your hand!");
                    } else {
                        try {
                            RegistryOps<Tag> nbtOps = RegistryOps.create(NbtOps.INSTANCE, player.registryAccess());
                            Tag tag = ItemStack.CODEC.encodeStart(nbtOps, stack).getOrThrow();
                            Component text = NbtFormatter.format(tag, true, 2, Integer.MAX_VALUE, true);
                            sendModMessage(text);
                        } catch (IllegalStateException e) {
                            sendModMessage("An error occurred while parsing the Nbt-Data!");
                            LogUtils.getLogger().error("An error occurred while parsing the Nbt-Data!", e);
                        }
                    }
                    return 1;
                }).then(literal("set-key").executes(c -> {
                    EdenClient.getMod(Scheduler.class).scheduleSyncDelayed(() ->
                            Minecraft.getInstance().setScreen(new PressKeyScreen(k -> NbtInfo.this.key = k)), 1);
                    return 1;
                })),
                "E-NBT displays all the Nbt-Data of the item you are currently holding.");
    }

    private boolean onInventoryKey(KeyEvent event, AbstractContainerScreen<?> screen) {
        if (key == GLFW.GLFW_KEY_ESCAPE) {
            return false;
        }
        if (event.key() != key) {
            return false;
        }
        Slot slot = ((HoveredSlotAccessor) screen).edenClient$getHoveredSlot();
        if (slot == null) {
            return false;
        }
        ItemStack stack = slot.getItem();
        if (stack.isEmpty()) {
            return false;
        }

        RegistryOps<Tag> nbtOps = RegistryOps.create(NbtOps.INSTANCE, PlayerUtils.getPlayer().registryAccess());
        Tag tag = ItemStack.CODEC.encodeStart(nbtOps, stack).getOrThrow();
        Component text = NbtFormatter.format(tag, true, 2, Integer.MAX_VALUE, true);
        ShowTextScreen showTextScreen = new ShowTextScreen(text);
        EdenClient.getMod(Scheduler.class).scheduleSyncDelayed(() -> Minecraft.getInstance().setScreen(showTextScreen), 1);
        return true;
    }
}
