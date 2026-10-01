package at.haha007.edenclient.mods;

import at.haha007.edenclient.EdenClient;
import at.haha007.edenclient.annotations.Mod;
import at.haha007.edenclient.utils.BlockArgument;
import at.haha007.edenclient.utils.EdenStreams;
import at.haha007.edenclient.utils.Scheduler;
import at.haha007.edenclient.utils.tasks.TaskManager;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.LevelChunk;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static at.haha007.edenclient.command.CommandManager.*;
import static at.haha007.edenclient.utils.PlayerUtils.sendModMessage;

@Mod(dependencies = {Scheduler.class, GetTo.class})
public class FindBlock {
    private TaskManager search;

    private FindBlock() {
        registerCommand();
    }

    private void registerCommand() {
        LiteralArgumentBuilder<FabricClientCommandSource> cmd = literal("efindblock");
        cmd.then(argument("block", new BlockArgument()).executes(ctx ->
                findBlock(ctx.getArgument("block", Block.class))));

        cmd.executes(_ -> {
            sendModMessage("/efindblock <block>");
            return Command.SINGLE_SUCCESS;
        });

        register(cmd,
                "Searches all loaded chunks for the nearest block of a type, then highlights it.",
                "Use \"/efindblock <block>\"");
    }

    private int findBlock(Block block) {
        if (block == Blocks.AIR) {
            sendModMessage("Unknown block.");
            return 0;
        }
        if (search != null) search.cancel();
        search = new TaskManager();
        search.then(() -> {
            ClientLevel level = Minecraft.getInstance().level;
            LocalPlayer player = Minecraft.getInstance().player;
            if (level == null || player == null) {
                sendModMessage("Cannot search for blocks while not in a world");
                return;
            }
            long deadline = System.currentTimeMillis() + Duration.ofSeconds(10).toMillis();
            ChunkPos center = player.chunkPosition();
            boolean empty = EdenStreams.streamOutwards(center).filter(chunkPos -> {
                LevelChunk chunk = level.getChunkSource().getChunkNow(chunkPos.x(), chunkPos.z());
                if (chunk == null || chunk.isEmpty()) {
                    return true;
                }
                if (System.currentTimeMillis() > deadline) {
                    sendModMessage("Search timed out");
                    return true;
                }

                List<BlockPos> positions = new ArrayList<>();
                chunk.findBlocks(state -> state.getBlock() == block, (a, _) -> positions.add(a.immutable()));
                if (positions.isEmpty()) {
                    return false;
                }
                BlockPos playerPos = player.blockPosition();
                int closestDistance = Integer.MAX_VALUE;
                BlockPos nearest = positions.getFirst();
                for (BlockPos position : positions) {
                    int distance = position.distManhattan(playerPos);
                    if (distance >= closestDistance) {
                        continue;
                    }
                    closestDistance = distance;
                    nearest = position;
                }
                sendModMessage("Found " + block.getName().getString() + " at " + nearest.getX() + " " + nearest.getY() + " " + nearest.getZ());
                GetTo getTo = EdenClient.getMod(GetTo.class);
                getTo.getTo(nearest, true, true, false);
                return true;
            }).findFirst().isEmpty();
            if (empty) {
                sendModMessage("No blocks found");
            }
        });
        search.start();
        return Command.SINGLE_SUCCESS;
    }
}
