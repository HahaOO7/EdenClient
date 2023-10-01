package at.haha007.edenclient.mods;

import at.haha007.edenclient.annotations.Mod;
import at.haha007.edenclient.callbacks.JoinWorldCallback;
import at.haha007.edenclient.callbacks.PlayerTickCallback;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;
import java.util.stream.Stream;

import static at.haha007.edenclient.command.CommandManager.literal;
import static at.haha007.edenclient.command.CommandManager.register;
import static at.haha007.edenclient.utils.PlayerUtils.getPlayer;
import static at.haha007.edenclient.utils.PlayerUtils.sendModMessage;

@Mod
public class AutoMoss {
    private boolean enabled;
    private int tick = 0;

    public AutoMoss() {
        PlayerTickCallback.EVENT.register(this::tick);
        JoinWorldCallback.EVENT.register(this::build);
        registerCommand();
    }

    private void tick(LocalPlayer player) {
        if (!enabled || ((tick++) % 3 != 0)) {
            return;
        }
        if (player.getInventory().getSelected().getItem() != Items.BONE_MEAL)
            return;
        ClientLevel world = player.clientLevel;
        Optional<BlockPos> moss = getNearby(player)
                .filter(bp -> world.getBlockState(bp).getBlock() == Blocks.MOSS_BLOCK)
                .filter(bp -> world.getBlockState(bp.offset(0, 1, 0)).getBlock() == Blocks.AIR)
                .filter(this::hasStoneNeighbor)
                .findAny();

        moss.ifPresent(this::clickPos);
    }

    private boolean hasStoneNeighbor(BlockPos blockPos) {
        ClientLevel world = getPlayer().clientLevel;
        ResourceKey<? extends Registry<Block>> registry = BlockTags.MOSS_REPLACEABLE.registry();
        RegistryAccess manager = world.registryAccess();
        for (BlockPos pos : BlockPos.betweenClosed(blockPos.offset(-1, -1, -1), blockPos.offset(1, 1, 1))) {
            ResourceLocation id = BuiltInRegistries.BLOCK.getKey(world.getBlockState(pos).getBlock());
            if (manager.registryOrThrow(registry).containsKey(id) && world.getBlockState(pos.offset(0, 1, 0)).getBlock() == Blocks.AIR)
                return true;
        }
        return false;
    }

    private Stream<BlockPos> getNearby(LocalPlayer player) {
        BlockPos pos = player.blockPosition();
        int dist = 5;
        return BlockPos.withinManhattanStream(pos, dist, dist, dist);
    }

    private void clickPos(Vec3i target) {
        BlockPos bp = new BlockPos(target);
        Direction dir = Direction.UP;
        MultiPlayerGameMode im = Minecraft.getInstance().gameMode;
        if (im == null) return;
        im.useItemOn(getPlayer(), InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(bp.relative(dir)), dir, bp, false));
    }

    private void build() {
        enabled = false;
    }

    private void registerCommand() {
        LiteralArgumentBuilder<ClientSuggestionProvider> cmd = literal("eautomoss");
        cmd.executes(c -> {
            enabled = !enabled;
            sendModMessage(enabled ? "AutoMoss enabled" : "AutoMoss disabled");
            return 1;
        });

        register(cmd, "AutoMoss is a mod to spread moss.");
    }
}
