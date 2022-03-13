package at.haha007.edenclient.mods;

import at.haha007.edenclient.callbacks.PlayerInteractBlockCallback;
import at.haha007.edenclient.utils.ChatColor;
import at.haha007.edenclient.utils.config.ConfigSubscriber;
import at.haha007.edenclient.utils.config.PerWorldConfig;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.fabricmc.fabric.impl.registry.sync.packet.RegistryPacketHandler;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.data.server.BlockTagProvider;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.BlockEventS2CPacket;
import net.minecraft.tag.BlockTags;
import net.minecraft.tag.Tag;
import net.minecraft.tag.TagKey;
import net.minecraft.tag.TagPacketSerializer;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.registry.DynamicRegistryManager;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import org.spongepowered.asm.mixin.transformer.ext.IDecompiler;

import java.util.Set;

import static at.haha007.edenclient.command.CommandManager.literal;
import static at.haha007.edenclient.command.CommandManager.register;
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
        var node = literal("eantistrip");
        node.then(literal("toggle").executes(c -> {
            enabled = !enabled;
            sendModMessage(ChatColor.GOLD + (enabled ? "Enabled AntiStrip." : "Disabled AntiStrip."));
            return 1;
        }));

        register(node,
                "AntiStrip disables stripping of wood with any axe.");
    }

    private ActionResult onInteractBlock(ClientPlayerEntity player, ClientWorld world, Hand hand, BlockHitResult blockHitResult) {
        if (!enabled) return ActionResult.PASS;
        if (!axeItems.contains((hand == Hand.MAIN_HAND ? player.getMainHandStack() : player.getOffHandStack()).getItem()))
            return ActionResult.PASS;
        Identifier id = Registry.BLOCK.getId(world.getBlockState(blockHitResult.getBlockPos()).getBlock());
        RegistryKey<? extends Registry<Block>> registry = BlockTags.LOGS.registry();
        DynamicRegistryManager registryManager = world.getRegistryManager();
        return registryManager.get(registry).containsId(id) ? ActionResult.FAIL : ActionResult.PASS;
    }
}
