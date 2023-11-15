package at.haha007.edenclient.mods.chestshop.pathing;

import at.haha007.edenclient.mods.chestshop.ChestShopEntry;
import at.haha007.edenclient.mods.chestshop.ChestShopMap;
import at.haha007.edenclient.mods.chestshop.ChestShopSet;
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
import net.minecraft.world.level.block.SignBlock;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static at.haha007.edenclient.utils.PlayerUtils.sendModMessage;

public class ChestShopModPathing implements Runnable {

    private final TaskManager tm;
    private final ChestShopMap shops;
    private final int chunks;
    private final PathFinder pathFinder;

    public ChestShopModPathing(TaskManager tm, ChestShopMap shops, int i, PathFinder pathFinderMod) {
        this.tm = tm;
        this.shops = shops;
        this.chunks = i;
        this.pathFinder = pathFinderMod;
    }

    @Override
    public void run() {
        LocalPlayer player = PlayerUtils.getPlayer();
        List<ChestShopEntry> chestShopsInRange = getChestShopsInRange(player);

        if (!chestShopsInRange.isEmpty()) {
            ChestShopModPathingMode mode = ChestShopModPathingMode.selectMode(chestShopsInRange);
            sendModMessage(ChatColor.GOLD + "Using mode: " + ChatColor.AQUA + mode.name());
            ChestShopEntry[] sortedEntries = mode.getPath(chestShopsInRange);
            for (ChestShopEntry entry : sortedEntries) {
                pathFinder.addPositionToVisit(entry.getPos(), () -> runEntryChestClick(entry));
            }
        }
    }

    private void runEntryChestClick(ChestShopEntry entry) {
        LocalPlayer player = PlayerUtils.getPlayer();
        BlockState signBlockState = player.clientLevel.getBlockState(new BlockPos(entry.getPos().getX(), entry.getPos().getY(), entry.getPos().getZ()));

        // Check if the block is a sign
        if (!(signBlockState.getBlock() instanceof SignBlock)) {
            sendModMessage("Stored ChestShop is not a sign block? " + entry);
            return;
        }

        // It's a sign, now get the block it is attached to
        BlockPos containerPos = null;

        if (signBlockState.getBlock() instanceof WallSignBlock) {
            // For wall signs, find the block it's attached to based on its facing direction
            Direction facing = signBlockState.getValue(WallSignBlock.FACING);
            containerPos = new BlockPos(entry.getPos().getX() - facing.getStepX(), entry.getPos().getY(), entry.getPos().getZ() - facing.getStepZ());
        } else if (signBlockState.getBlock() instanceof StandingSignBlock) {
            // For standing signs, the block below is what the sign is standing on
            containerPos = new BlockPos(entry.getPos().getX(), entry.getPos().getY() - 1, entry.getPos().getZ());
        }

        if (containerPos == null) {
            sendModMessage("Couldn't find container the sign is attached to? " + entry);
            return;
        }

        MultiPlayerGameMode interactionManager = Objects.requireNonNull(Minecraft.getInstance().gameMode);

        interactionManager.startDestroyBlock(new BlockPos(containerPos.getX(), containerPos.getY(), containerPos.getZ()), getFacingDirection(player, containerPos));
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
        return allShops;
    }
}
