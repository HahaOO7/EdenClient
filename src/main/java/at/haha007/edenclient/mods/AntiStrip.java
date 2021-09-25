package at.haha007.edenclient.mods;

import at.haha007.edenclient.callbacks.PlayerInteractBlockCallback;
import at.haha007.edenclient.command.CommandManager;
import at.haha007.edenclient.utils.config.ConfigSubscriber;
import at.haha007.edenclient.utils.config.PerWorldConfig;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.tag.BlockTags;
import net.minecraft.text.LiteralText;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;

import java.util.Set;

import static at.haha007.edenclient.utils.PlayerUtils.sendModMessage;

public class AntiStrip {
    private final Set<Item> axeItems = Set.of(
            Items.WOODEN_AXE,
            Items.STONE_AXE,
            Items.IRON_AXE,
            Items.GOLDEN_AXE,
            Items.DIAMOND_AXE,
            Items.NETHERITE_AXE
    );

    @ConfigSubscriber("false")
    private boolean enabled = true;

    public AntiStrip() {
        PlayerInteractBlockCallback.EVENT.register(this::onInteractBlock);
        PerWorldConfig.get().register(this, "antiStrip");
        registerCommand();
    }

    private void registerCommand() {
        CommandManager.register(CommandManager.literal("antistrip").executes(c -> {
            enabled = !enabled;
            sendModMessage(new LiteralText(enabled ? "Enabled AntiStrip." : "Disabled AntiStrip.").formatted(Formatting.GOLD));
            return 1;
        }));
    }

    private ActionResult onInteractBlock(ClientPlayerEntity player, ClientWorld world, Hand hand, BlockHitResult blockHitResult) {
        if (!enabled) return ActionResult.PASS;
        if (!axeItems.contains((hand == Hand.MAIN_HAND ? player.getMainHandStack() : player.getOffHandStack()).getItem()))
            return ActionResult.PASS;


        return BlockTags.LOGS.contains(world.getBlockState(blockHitResult.getBlockPos()).getBlock()) ?
                ActionResult.FAIL : ActionResult.PASS;
    }
}
