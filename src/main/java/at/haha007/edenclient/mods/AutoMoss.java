package at.haha007.edenclient.mods;

import at.haha007.edenclient.callbacks.JoinWorldCallback;
import at.haha007.edenclient.callbacks.PlayerTickCallback;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.block.AirBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientCommandSource;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.item.Items;
import net.minecraft.tag.BlockTags;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.util.registry.DynamicRegistryManager;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;

import java.util.Optional;
import java.util.stream.Stream;

import static at.haha007.edenclient.command.CommandManager.literal;
import static at.haha007.edenclient.command.CommandManager.register;
import static at.haha007.edenclient.utils.PlayerUtils.getPlayer;
import static at.haha007.edenclient.utils.PlayerUtils.sendModMessage;

public class AutoMoss {
    private boolean enabled;
    private int tick = 0;

    public AutoMoss() {
        PlayerTickCallback.EVENT.register(this::tick);
        JoinWorldCallback.EVENT.register(this::build);
        registerCommand();
    }

    private void tick(ClientPlayerEntity player) {
        if (!enabled || ((tick++) % 3 != 0)) {
            return;
        }
        if (player.getInventory().getMainHandStack().getItem() != Items.BONE_MEAL)
            return;
        ClientWorld world = player.clientWorld;
        Optional<BlockPos> moss = getNearby(player)
                .filter(bp -> world.getBlockState(bp).getBlock() == Blocks.MOSS_BLOCK)
                .filter(bp -> world.getBlockState(bp.add(0, 1, 0)).getBlock() == Blocks.AIR)
                .filter(this::hasStoneNeighbor)
                .findAny();

        moss.ifPresent(this::clickPos);
    }

    private boolean hasStoneNeighbor(BlockPos blockPos) {
        ClientWorld world = getPlayer().clientWorld;
        RegistryKey<? extends Registry<Block>> registry = BlockTags.MOSS_REPLACEABLE.registry();
        DynamicRegistryManager manager = world.getRegistryManager();
        for (BlockPos pos : BlockPos.iterate(blockPos.add(-1, -1, -1), blockPos.add(1, 1, 1))) {
            Identifier id = Registry.BLOCK.getId(world.getBlockState(pos).getBlock());
            if (manager.get(registry).containsId(id) && world.getBlockState(pos.add(0, 1, 0)).getBlock() == Blocks.AIR)
                return true;
        }
        return false;
    }

    private Stream<BlockPos> getNearby(ClientPlayerEntity player) {
        BlockPos pos = player.getBlockPos();
        int dist = 5;
        return BlockPos.streamOutwards(pos, dist, dist, dist);
    }

    private void clickPos(Vec3i target) {
        BlockPos bp = new BlockPos(target);
        Direction dir = Direction.UP;
        ClientPlayerInteractionManager im = MinecraftClient.getInstance().interactionManager;
        if (im == null) return;
        im.interactBlock(getPlayer(), Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(bp.offset(dir)), dir, bp, false));
    }

    private void build() {
        enabled = false;
    }

    private void registerCommand() {
        LiteralArgumentBuilder<ClientCommandSource> cmd = literal("eautomoss");
        cmd.executes(c -> {
            enabled = !enabled;
            sendModMessage(enabled ? "AutoMoss enabled" : "AutoMoss disabled");
            return 1;
        });

        register(cmd, "AutoMoss is a mod to spread moss.");
    }
}
