package at.haha007.edenclient.mods;

import at.haha007.edenclient.annotations.Mod;
import at.haha007.edenclient.callbacks.JoinWorldCallback;
import at.haha007.edenclient.callbacks.PlayerTickCallback;
import at.haha007.edenclient.utils.PlayerUtils;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.GrindstoneScreen;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Holder;
import net.minecraft.data.tags.EnchantmentTagsProvider;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.GrindstoneMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import java.util.concurrent.ThreadLocalRandom;

import static at.haha007.edenclient.command.CommandManager.literal;
import static at.haha007.edenclient.command.CommandManager.register;

@Mod
public class AotoGrindstone {
    private static final int RESULT_SLOT = 2;

    boolean enabled = false;

    public AotoGrindstone() {
        registerCommand();
        PlayerTickCallback.EVENT.register(this::onTick, getClass());
        JoinWorldCallback.EVENT.register(this::onJoin, getClass());
    }

    private void onJoin() {
        enabled = false;
    }

    private void registerCommand() {
        LiteralArgumentBuilder<ClientSuggestionProvider> node = literal("eautogrindstone");
        node.then(literal("toggle").executes(c -> {
            enabled = !enabled;
            PlayerUtils.sendModMessage((enabled ? "AutoGrindstone enabled" : "AutoGrindstone disabled"));
            return 1;
        }));
        register(node, "AutoGrindstone automatically throws all items in your inventory into a grindstone.");
    }

    private void onTick(LocalPlayer player) {
        if (!this.enabled)
            return;
        Screen screen = Minecraft.getInstance().screen;

        if (!(screen instanceof GrindstoneScreen grindstoneScreen)) {
            return;
        }

        GrindstoneMenu menu = grindstoneScreen.getMenu();
        int windowId = menu.containerId;
        int state = menu.getStateId() + ThreadLocalRandom.current().nextInt(10000) + 100;
        boolean hasItemInGrindstone = (!menu.getSlot(0).getItem().isEmpty() || !menu.getSlot(1).getItem().isEmpty());
        for (int i = 3; i < menu.slots.size() && !hasItemInGrindstone; i++) {
            ItemStack item = menu.getSlot(i).getItem();
            if (item.isEmpty() || !isItemEnchanted(item))
                continue;
            player.connection.send(new ServerboundContainerClickPacket(windowId, state, i, 0,
                    ClickType.QUICK_MOVE, item, Int2ObjectMaps.emptyMap()));
            return;
        }
        ItemStack item = menu.getSlot(RESULT_SLOT).getItem();
        player.connection.send(new ServerboundContainerClickPacket(windowId, state, RESULT_SLOT, 0,
                ClickType.THROW, item, Int2ObjectMaps.emptyMap()));
    }

    private boolean isItemEnchanted(ItemStack item) {
        if (!item.isEnchanted())
            return false;
        ItemEnchantments enchantments = item.getEnchantments();
        if (enchantments.isEmpty())
            return false;
        return enchantments.keySet().stream().anyMatch(e -> e.is(EnchantmentTags.CURSE));
    }
}
