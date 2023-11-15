package at.haha007.edenclient.mods.chestshop.pathing;

import at.haha007.edenclient.mods.chestshop.ChestShopEntry;
import at.haha007.edenclient.utils.PlayerUtils;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public enum ChestShopModPathingMode {

    /**
     * GO FROM BOTTOM TO TOP. FOR EACH HEIGHT THE SHOPS ARE SORTED EFFICIENTLY.
     */
    BOTTOM_TO_TOP {
        @Override
        ChestShopEntry[] getPath(List<ChestShopEntry> entries) {
            Map<Integer, List<ChestShopEntry>> heightMap = entries.stream()
                    .collect(Collectors.groupingBy(c -> c.getPos().getY()));

            // for the first chest choose the closest to the player on the lowest level
            Vec3i lastShopAtPreviousHeight = new Vec3i(PlayerUtils.getPlayer().getBlockX(), PlayerUtils.getPlayer().getBlockY(), PlayerUtils.getPlayer().getBlockZ());

            List<ChestShopEntry> sortedFinalList = new ArrayList<>();

            for (Integer height : heightMap.keySet().stream().sorted().toList()) {
                List<ChestShopEntry> unsortedShops = heightMap.get(height);

                List<ChestShopEntry> sortedShops = sortEntriesWithStartPos(unsortedShops, lastShopAtPreviousHeight);
                if (!sortedShops.isEmpty()) {
                    lastShopAtPreviousHeight = sortedShops.get(sortedShops.size() - 1).getPos();
                }

                sortedFinalList.addAll(sortedShops);
            }

            return sortedFinalList.toArray(new ChestShopEntry[0]);
        }
    },

    CHUNKWISE {
        @Override
        ChestShopEntry[] getPath(List<ChestShopEntry> entries) {
            List<ChestShopEntry> sortedEntries = new ArrayList<>();
            Map<ChunkPos, List<ChestShopEntry>> map = entries.stream().collect(Collectors.groupingBy(ChestShopEntry::getChunkPos));
            List<ChunkPos> chunkTraverseOrder = getChunkTraverseOrder(map);

            Vec3i lastEntryOfChunk = null;

            for (ChunkPos chunkPos : chunkTraverseOrder) {
                List<ChestShopEntry> shopsForChunk = map.get(chunkPos);
                if (shopsForChunk.isEmpty()) {
                    continue; // shouldn't happen
                }
                List<ChestShopEntry> sortedShopsForChunk = ChestShopModPathingMode.sortEntriesWithStartPos(shopsForChunk, lastEntryOfChunk);
                ChestShopEntry last = sortedShopsForChunk.get(sortedShopsForChunk.size() - 1);
                lastEntryOfChunk = new Vec3i(last.getPos().getX(), last.getPos().getY(), last.getPos().getZ());

                sortedEntries.addAll(sortedShopsForChunk);
            }

            return sortedEntries.toArray(new ChestShopEntry[0]);
        }

        private List<ChunkPos> getChunkTraverseOrder(Map<ChunkPos, List<ChestShopEntry>> map) {
            List<ChunkPos> sortedChunkPos = new ArrayList<>();
            List<ChunkPos> unsortedChunkPos = new ArrayList<>(map.keySet());
            ChunkPos lastChunkPos = unsortedChunkPos.stream().min(Comparator.comparingDouble(ch -> PlayerUtils.getPlayer().distanceToSqr(new Vec3(ch.x, PlayerUtils.getPlayer().getBlockY(), ch.z)))).get();
            unsortedChunkPos.remove(lastChunkPos);
            sortedChunkPos.add(lastChunkPos);
            while (!unsortedChunkPos.isEmpty()) {
                ChunkPos finalLastChunkPos = lastChunkPos;
                ChunkPos closest = unsortedChunkPos.stream().min(Comparator.comparingDouble(ch -> ch.distanceSquared(finalLastChunkPos))).get();
                lastChunkPos = closest;
                unsortedChunkPos.remove(closest);
                sortedChunkPos.add(closest);
            }
            return sortedChunkPos;
        }
    };

    /*
    Sorts the shops into an efficient order by starting at the shop closest to the startPos and always going to the next closest shop.
   */
    private static List<ChestShopEntry> sortEntriesWithStartPos(List<ChestShopEntry> unsortedShops, Vec3i startPos) {
        unsortedShops = new ArrayList<>(unsortedShops); // make sure its mutable
        unsortedShops.sort(Comparator.comparingDouble(c -> c.getPos().distSqr(startPos))); // start with pos closest to the startPos!
        List<ChestShopEntry> sortedShops = new ArrayList<>();
        ChestShopEntry lastEntry = unsortedShops.get(0);
        sortedShops.add(lastEntry);
        unsortedShops.remove(0);
        while (!unsortedShops.isEmpty()) {
            ChestShopEntry finalLastEntry = lastEntry;
            ChestShopEntry closest = unsortedShops.stream().min(Comparator.comparingDouble(c -> c.getPos().distSqr(finalLastEntry.getPos()))).get();
            unsortedShops.remove(closest);
            lastEntry = closest;
            sortedShops.add(lastEntry);
        }
        return sortedShops;
    }


    public static ChestShopModPathingMode selectMode(List<ChestShopEntry> chestShopsInRange) {
        // if the chestshops are all at about the same height they are probably in a single room
        // --> therefore traversing the whole room more than once is a waste of time --> better to traverse chunkwise
        Integer lowestEntry = chestShopsInRange.stream().min(Comparator.comparingInt(c -> c.getPos().getY())).map(c -> c.getPos().getY()).get();
        Integer highestEntry = chestShopsInRange.stream().max(Comparator.comparingInt(c -> c.getPos().getY())).map(c -> c.getPos().getY()).get();
        if (highestEntry - lowestEntry <= 3 && highestEntry - lowestEntry != 0) {
            return ChestShopModPathingMode.CHUNKWISE;
        }

        // otherwise traverse once per height, from bottom to top
        return ChestShopModPathingMode.BOTTOM_TO_TOP;
    }

    /**
     * Returns the path for the entries.
     *
     * @param entries The entries to sort.
     * @return The sorted path order.
     */
    abstract ChestShopEntry[] getPath(List<ChestShopEntry> entries);
}
