package at.haha007.edenclient.mods.chestshop.pathing;

import at.haha007.edenclient.callbacks.AddChatMessageCallback;
import at.haha007.edenclient.mods.chestshop.ChestShopEntry;
import at.haha007.edenclient.mods.chestshop.ChestShopMap;
import at.haha007.edenclient.mods.chestshop.ChestShopSet;
import at.haha007.edenclient.mods.datafetcher.DataFetcher;
import at.haha007.edenclient.mods.pathfinder.PathFinder;
import at.haha007.edenclient.utils.ChatColor;
import at.haha007.edenclient.utils.PlayerUtils;
import at.haha007.edenclient.utils.tasks.TaskManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static at.haha007.edenclient.utils.PlayerUtils.sendModMessage;

public class ChestShopModPathing implements Runnable {

    private final TaskManager tm;
    private final ChestShopMap shops;
    private final int chunks;
    private final PathFinder pathFinder;

    private final DataFetcher dataFetcher;

    private ChestShopModPathingChatHandler chatHandler;

    private ChestShopEntry[] sortedEntries;

    public ChestShopModPathing(TaskManager tm, ChestShopMap shops, int i, PathFinder pathFinderMod, DataFetcher dataFetcher) {
        this.tm = tm;
        this.shops = shops;
        this.chunks = i;
        this.dataFetcher = dataFetcher;
        this.pathFinder = pathFinderMod;
    }

    @Override
    public void run() {
        LocalPlayer player = PlayerUtils.getPlayer();
        List<ChestShopEntry> chestShopsInRange = getChestShopsInRange(player);

        if (!chestShopsInRange.isEmpty()) {
            ChestShopModPathingMode mode = ChestShopModPathingMode.selectMode(chestShopsInRange);
            sendModMessage(ChatColor.GOLD + "Using mode: " + ChatColor.AQUA + mode.name());
            this.sortedEntries = mode.getPath(chestShopsInRange);
            chatHandler = new ChestShopModPathingChatHandler(sortedEntries, dataFetcher);
            for (ChestShopEntry entry : sortedEntries) {
                pathFinder.addPositionToVisit(entry.getPos(), () -> runEntryChestClick(entry));
            }
        }
    }

    public void onChat(AddChatMessageCallback.ChatAddEvent chatAddEvent) {
        if (chatHandler != null) {
            chatHandler.onChat(chatAddEvent);
        }
    }

    private void runEntryChestClick(ChestShopEntry entry) {
        LocalPlayer player = PlayerUtils.getPlayer();

        BlockState signBlockState = player.clientLevel.getBlockState(new BlockPos(entry.getPos().getX(), entry.getPos().getY(), entry.getPos().getZ()));
        BlockPos signBlockPos = new BlockPos(entry.getPos().getX(), entry.getPos().getY(), entry.getPos().getZ());

        // Check if the block is a sign
        if (!(signBlockState.getBlock() instanceof SignBlock)) {
            sendModMessage("Stored ChestShop is not a sign block? " + entry);
            return;
        }

        List<BlockPos> possibleContainerPositions = new ArrayList<>(List.of(
                new BlockPos(signBlockPos.getX(), signBlockPos.getY() - 1, signBlockPos.getZ()), // Below the sign
                new BlockPos(signBlockPos.getX(), signBlockPos.getY() + 1, signBlockPos.getZ()), // Above the sign
                new BlockPos(signBlockPos.getX() + 1, signBlockPos.getY(), signBlockPos.getZ()), // To the east of the sign
                new BlockPos(signBlockPos.getX(), signBlockPos.getY(), signBlockPos.getZ() - 1), // To the north of the sign
                new BlockPos(signBlockPos.getX() - 1, signBlockPos.getY(), signBlockPos.getZ()), // To the west of the sign
                new BlockPos(signBlockPos.getX(), signBlockPos.getY(), signBlockPos.getZ() + 1)  // To the south of the sign
        ));

        // if it is directly behind the sign
        if (signBlockState.getBlock() instanceof WallSignBlock) {
            // For wall signs, find the block it's attached to based on its facing direction
            Direction facing = signBlockState.getValue(WallSignBlock.FACING);
            BlockPos mostLikelyPos = new BlockPos(entry.getPos().getX() - facing.getStepX(), entry.getPos().getY(), entry.getPos().getZ() - facing.getStepZ());
            possibleContainerPositions.add(0, mostLikelyPos);
        }

        for (BlockPos pos : possibleContainerPositions) {
            BlockState state = PlayerUtils.getPlayer().clientLevel.getBlockState(pos);
            Block block = state.getBlock();
            if (block == Blocks.CHEST || block == Blocks.TRAPPED_CHEST) {
                MultiPlayerGameMode interactionManager = Objects.requireNonNull(Minecraft.getInstance().gameMode);
                interactionManager.startDestroyBlock(pos, getFacingDirection(player, pos));
                entry.setChestPos(pos);
                entry.setChestBlockType(state.getValue(ChestBlock.TYPE));
                break;
            }
        }
    }

    private Direction getFacingDirection(LocalPlayer player, BlockPos blockPos) {
        Vec3 playerPosition = player.getEyePosition(1.0F); // Player's eye position
        Vec3 blockCenter = Vec3.atCenterOf(blockPos); // Center position of the block
        Vec3 directionVector = playerPosition.subtract(blockCenter); // Direction vector

        // Determine the dominant component in the direction vector
        double absX = Math.abs(directionVector.x);
        double absY = Math.abs(directionVector.y);
        double absZ = Math.abs(directionVector.z);

        if (absX > absY && absX > absZ) {
            // X is dominant
            return directionVector.x > 0 ? Direction.EAST : Direction.WEST;
        } else if (absY > absZ) {
            // Y is dominant
            return directionVector.y > 0 ? Direction.UP : Direction.DOWN;
        } else {
            // Z is dominant
            return directionVector.z > 0 ? Direction.SOUTH : Direction.NORTH;
        }
    }

    /*
    Gets all chest shops in the players radius.
    */
    private List<ChestShopEntry> getChestShopsInRange(LocalPlayer player) {
        List<ChestShopEntry> allShops = new ArrayList<>();
        ChunkPos.rangeClosed(player.chunkPosition(), chunks).forEach(cp -> {
            if (shops.containsKey(cp)) {
                ChestShopSet chestShops = shops.get(cp);
                allShops.addAll(chestShops);
            }
        });
        return allShops.stream().filter(ChestShopEntry::isShop).collect(Collectors.toList());
    }
}
