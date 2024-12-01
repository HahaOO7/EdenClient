package at.haha007.edenclient.mods;

import at.haha007.edenclient.annotations.Mod;
import at.haha007.edenclient.callbacks.ConfigLoadedCallback;
import at.haha007.edenclient.callbacks.PlayerAttackBlockCallback;
import at.haha007.edenclient.utils.PlayerUtils;
import at.haha007.edenclient.utils.config.ConfigSubscriber;
import at.haha007.edenclient.utils.config.PerWorldConfig;
import at.haha007.edenclient.utils.config.wrappers.BlockSet;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;

import static at.haha007.edenclient.command.CommandManager.*;

@Mod
public class EnsureSilk {

    @ConfigSubscriber
    private boolean enabled = false;
    @ConfigSubscriber
    BlockSet filter = new BlockSet();

    public EnsureSilk() {
        registerCommand();
        PerWorldConfig.get().register(this, "EnsureSilk");
        PlayerAttackBlockCallback.EVENT.register(this::onAttackBlock, getClass());
        ConfigLoadedCallback.EVENT.register(this::configLoaded, getClass());
    }

    private void configLoaded() {
        if (filter == null) {
            filter = new BlockSet();
        }
    }

    private InteractionResult onAttackBlock(LocalPlayer localPlayer, BlockPos blockPos, Direction direction) {
        if (!enabled) return InteractionResult.PASS;
        Block block = localPlayer.clientLevel.getBlockState(blockPos).getBlock();
        if (!filter.contains(block)) return InteractionResult.PASS;
        ItemStack tool = localPlayer.getMainHandItem();
        if (tool.isEmpty()) return InteractionResult.FAIL;
        Holder.Reference<Enchantment> silk = localPlayer.clientLevel.registryAccess().lookup(Registries.ENCHANTMENT).orElseThrow().get(Enchantments.SILK_TOUCH).orElseThrow();
        boolean hasSilkTouch = tool.getEnchantments().getLevel(silk) > 0;
        return hasSilkTouch ? InteractionResult.PASS : InteractionResult.FAIL;
    }

    private void registerCommand() {
        LiteralArgumentBuilder<FabricClientCommandSource> node = literal("eensuresilk");
        node.then(literal("toggle").executes(c -> {
            enabled = !enabled;
            PlayerUtils.sendModMessage((enabled ? "SilkTouch enabled" : "SilkTouch disabled"));
            return 1;
        }));
        node.then(addCommand());
        node.then(removeCommand());
        node.then(literal("clear").executes(c -> {
            filter.clear();
            PlayerUtils.sendModMessage("Cleared filter");
            return 1;
        }));
        register(node, "Ensures blocks are broken with silk touch.");
    }


    private ArgumentBuilder<FabricClientCommandSource, ?> addCommand() {
        LiteralArgumentBuilder<FabricClientCommandSource> cmd = literal("add");
        BuiltInRegistries.BLOCK.forEach(block -> {
            String name = BuiltInRegistries.BLOCK.getKey(block).getPath();
            cmd.then(literal(name).executes(context -> {
                filter.add(block);
                PlayerUtils.sendModMessage("Added " + name);
                return 1;
            }));
        });
        return cmd;
    }

    private ArgumentBuilder<FabricClientCommandSource, ?> removeCommand() {
        LiteralArgumentBuilder<FabricClientCommandSource> cmd = literal("remove");
        cmd.then(argument("type", StringArgumentType.word()).suggests((context, builder) -> {
            for (Block block : filter) {
                builder.suggest(BuiltInRegistries.BLOCK.getKey(block).getPath());
            }
            return builder.buildFuture();
        }).executes(context -> {
            String name = context.getArgument("type", String.class);
            ResourceLocation identifier = ResourceLocation.parse(name);
            filter.remove(BuiltInRegistries.BLOCK.getValue(identifier));
            PlayerUtils.sendModMessage("Removed " + name);
            return 1;
        }));
        return cmd;
    }
}
