package at.haha007.edenclient.mods;

import at.haha007.edenclient.callbacks.ConfigLoadCallback;
import at.haha007.edenclient.callbacks.ConfigSaveCallback;
import at.haha007.edenclient.callbacks.PlayerInteractBlockEvent;
import at.haha007.edenclient.command.Command;
import at.haha007.edenclient.command.CommandManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.tag.BlockTags;
import net.minecraft.text.LiteralText;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;

import java.util.Collection;
import java.util.HashSet;

public class AntiStrip {
    private boolean enabled = true;
    private final Collection<Item> axeItems = new HashSet<>();

    public AntiStrip() {
        PlayerInteractBlockEvent.EVENT.register(this::onInteractBlock);
        ConfigLoadCallback.EVENT.register(this::loadCfg);
        ConfigSaveCallback.EVENT.register(this::saveCfg);
        CommandManager.registerCommand(new Command(this::onCommand), "antistrip");
        axeItems.add(Items.WOODEN_AXE);
        axeItems.add(Items.STONE_AXE);
        axeItems.add(Items.IRON_AXE);
        axeItems.add(Items.GOLDEN_AXE);
        axeItems.add(Items.DIAMOND_AXE);
        axeItems.add(Items.NETHERITE_AXE);
    }

    private ActionResult loadCfg(NbtCompound compoundTag) {
        NbtCompound tag = compoundTag.getCompound("antiStrip");
        enabled = !tag.contains("enabled") || tag.getBoolean("enabled");
        return ActionResult.PASS;
    }

    private ActionResult saveCfg(NbtCompound compoundTag) {
        NbtCompound tag = compoundTag.getCompound("antiStrip");
        tag.putBoolean("enabled", enabled);
        compoundTag.put("antiStrip", tag);
        return ActionResult.PASS;
    }

    private void onCommand(Command cmd, String label, String[] args) {
        enabled = !enabled;
        MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(new LiteralText(enabled ? "Enabled AntiStrip." : "Disabled AntiStrip.").formatted(Formatting.AQUA));
    }

    private ActionResult onInteractBlock(ClientPlayerEntity player, ClientWorld world, Hand hand, BlockHitResult blockHitResult) {
        if (!enabled) return ActionResult.PASS;
        if (!axeItems.contains((hand == Hand.MAIN_HAND ? player.getMainHandStack() : player.getOffHandStack()).getItem()))
            return ActionResult.PASS;


        return BlockTags.LOGS.contains(world.getBlockState(blockHitResult.getBlockPos()).getBlock()) ?
                ActionResult.FAIL : ActionResult.PASS;
    }
}
