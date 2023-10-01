package at.haha007.edenclient.mods;

import at.haha007.edenclient.annotations.Mod;
import at.haha007.edenclient.callbacks.PlayerInteractBlockCallback;
import at.haha007.edenclient.utils.ChatColor;
import at.haha007.edenclient.utils.config.ConfigSubscriber;
import at.haha007.edenclient.utils.config.PerWorldConfig;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;

import java.util.Set;

import static at.haha007.edenclient.command.CommandManager.literal;
import static at.haha007.edenclient.command.CommandManager.register;
import static at.haha007.edenclient.utils.PlayerUtils.sendModMessage;

@Mod
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
        var node = literal("eantistrip");
        node.then(literal("toggle").executes(c -> {
            enabled = !enabled;
            sendModMessage(ChatColor.GOLD + (enabled ? "Enabled AntiStrip." : "Disabled AntiStrip."));
            return 1;
        }));

        register(node,
                "AntiStrip disables stripping of wood with any axe.");
    }

    private InteractionResult onInteractBlock(LocalPlayer player, ClientLevel world, InteractionHand hand, BlockHitResult blockHitResult) {
        if (!enabled) return InteractionResult.PASS;
        if (player.isCreative()) return InteractionResult.PASS;
        if (!axeItems.contains((hand == InteractionHand.MAIN_HAND ? player.getMainHandItem() : player.getOffhandItem()).getItem()))
            return InteractionResult.PASS;
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(world.getBlockState(blockHitResult.getBlockPos()).getBlock());
        RegistryAccess registryManager = world.registryAccess();
        ResourceKey<? extends Registry<Block>> logsRegistry = BlockTags.LOGS.registry();
        return registryManager.registryOrThrow(logsRegistry).containsKey(id) ? InteractionResult.FAIL : InteractionResult.PASS;
    }
}
