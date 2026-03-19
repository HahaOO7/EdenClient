package at.haha007.edenclient.utils.pathing;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.*;

public class SimplePathFinder {

    private static class PathNode implements Comparable<PathNode> {
        final BlockPos position;
        final double gCost; // Distance from start
        final double hCost; // Heuristic distance to end
        final double fCost; // Total cost
        final PathNode parent;

        PathNode(BlockPos position, double gCost, double hCost, PathNode parent) {
            this.position = position;
            this.gCost = gCost;
            this.hCost = hCost;
            this.fCost = gCost + hCost;
            this.parent = parent;
        }

        @Override
        public int compareTo(PathNode other) {
            return Double.compare(this.fCost, other.fCost);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            PathNode pathNode = (PathNode) obj;
            return Objects.equals(position, pathNode.position);
        }

        @Override
        public int hashCode() {
            return Objects.hash(position);
        }
    }

    public static Path findPath(Vec3i start, Vec3i end) {
        if (start.getY() != end.getY()) {
            return null;
        }
        BlockPos startingBlock = new BlockPos(start);
        BlockPos endingBlock = new BlockPos(end);

        PriorityQueue<PathNode> openSet = new PriorityQueue<>();
        Set<BlockPos> closedSet = new HashSet<>();
        Map<BlockPos, PathNode> nodeMap = new HashMap<>();

        double initialHCost = heuristic(startingBlock, endingBlock);
        PathNode startNode = new PathNode(startingBlock, 0, initialHCost, null);
        openSet.add(startNode);
        nodeMap.put(startingBlock, startNode);

        while (!openSet.isEmpty()) {
            PathNode currentNode = openSet.poll();

            if (currentNode.position.equals(endingBlock)) {
                return reconstructPath(currentNode);
            }

            closedSet.add(currentNode.position);

            for (Direction direction : Direction.values()) {
                BlockPos neighborPos = currentNode.position.relative(direction);

                if (closedSet.contains(neighborPos) || !isWalkable(neighborPos)) {
                    continue;
                }

                double tentativeGCost = currentNode.gCost + getMovementCost(currentNode.position, neighborPos);

                PathNode existingNode = nodeMap.get(neighborPos);
                if (existingNode == null) {
                    double hCost = heuristic(neighborPos, endingBlock);
                    PathNode neighborNode = new PathNode(neighborPos, tentativeGCost, hCost, currentNode);
                    openSet.add(neighborNode);
                    nodeMap.put(neighborPos, neighborNode);
                } else if (tentativeGCost < existingNode.gCost) {
                    openSet.remove(existingNode);
                    PathNode updatedNode = new PathNode(neighborPos, tentativeGCost, existingNode.hCost, currentNode);
                    openSet.add(updatedNode);
                    nodeMap.put(neighborPos, updatedNode);
                }
            }
        }
        return null;
    }

    private static double heuristic(BlockPos from, BlockPos to) {
        // Manhattan distance is admissible for grid-based movement
        return Math.abs(from.getX() - to.getX()) + Math.abs(from.getZ() - to.getZ());
    }

    private static double getMovementCost(BlockPos from, BlockPos to) {
        // Base cost of 1 for horizontal movement
        // Could be modified to account for different terrain types
        return 1.0;
    }

    // returns all blocks a player would collide with walking a straight line
    // a player is 0.6x0.6 blocks wide
    // only blocks at foot height in a horizontal line
    private static Collection<BlockPos> getBlocksAlongLine(Vec3 start, Vec3 end) {
        Set<BlockPos> blocks = new HashSet<>();

        // Player collision box dimensions (only width matters for horizontal movement)
        final double PLAYER_WIDTH = 0.6;

        // Use the foot height (y-coordinate) from the start position
        int footY = (int) Math.floor(start.y);

        // Calculate the horizontal distance
        double dx = end.x - start.x;
        double dz = end.z - start.z;
        double distance = Math.sqrt(dx * dx + dz * dz);

        if (distance < 0.01) {
            // Start and end are very close, just check the start position
            return getBlocksAtFootLevel(start.x, start.z, footY);
        }

        // Step size - smaller steps for more accuracy
        double stepSize = 0.1;
        int numSteps = (int) (distance / stepSize) + 1;

        // Check blocks along the horizontal line
        for (int i = 0; i <= numSteps; i++) {
            double t = (double) i / numSteps;
            double currentX = start.x + dx * t;
            double currentZ = start.z + dz * t;

            // Add all blocks the player's collision box would intersect at this position
            blocks.addAll(getBlocksAtFootLevel(currentX, currentZ, footY));
        }

        return blocks;
    }

    // Helper method to get all blocks a player's collision box would intersect at foot level
    private static Collection<BlockPos> getBlocksAtFootLevel(double x, double z, int footY) {
        Set<BlockPos> blocks = new HashSet<>();

        // Player collision box width
        final double PLAYER_WIDTH = 0.7;

        // Calculate the bounds of the player's collision box at foot level
        double minX = x - PLAYER_WIDTH / 2;
        double maxX = x + PLAYER_WIDTH / 2;
        double minZ = z - PLAYER_WIDTH / 2;
        double maxZ = z + PLAYER_WIDTH / 2;

        // Add all blocks that intersect with the collision box at foot level
        int startBlockX = (int) Math.floor(minX);
        int endBlockX = (int) Math.floor(maxX);
        int startBlockZ = (int) Math.floor(minZ);
        int endBlockZ = (int) Math.floor(maxZ);

        for (int blockX = startBlockX; blockX <= endBlockX; blockX++) {
            for (int blockZ = startBlockZ; blockZ <= endBlockZ; blockZ++) {
                blocks.add(new BlockPos(blockX, footY, blockZ));
            }
        }

        return blocks;
    }

    private static Path reconstructPath(PathNode endNode) {
        List<BlockPos> path = new ArrayList<>();
        PathNode current = endNode;

        while (current != null) {
            path.add(current.position);
            current = current.parent;
        }

        Collections.reverse(path);
        optimizePath(path);
        return new Path(path);
    }

    // try to remove blocks that are not needed
    // by trying to remove blocks and checking if all the blocks in the path are still walkable
    // using the getBlocksAlongLine method
    private static void optimizePath(List<BlockPos> path) {
        if (path.size() <= 2) {
            return; // No optimization needed for very short paths
        }

        // Try to remove intermediate points
        int i = 0;
        while (i < path.size() - 2) {
            BlockPos start = path.get(i);
            BlockPos end = path.get(i + 2);

            // Get all blocks that would be traversed in a straight line
            Collection<BlockPos> blocksAlongLine = getBlocksAlongLine(Vec3.atBottomCenterOf(start), Vec3.atBottomCenterOf(end));

            // Check if all blocks along the line are walkable
            boolean allWalkable = true;
            for (BlockPos block : blocksAlongLine) {
                if (!isWalkable(block)) {
                    allWalkable = false;
                    break;
                }
            }

            if (allWalkable) {
                // Remove the intermediate point since the straight line is walkable
                path.remove(i + 1);
                // Don't increment i to try to optimize further
            } else {
                // Can't remove this point, move to next
                i++;
            }
        }
    }


    private static boolean isWalkable(BlockPos pos) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return false;
        }
        //floor has to have a solid top surface and 2 blocks without collision above
        BlockState floorState = level.getBlockState(pos.below());
        if (!floorState.isFaceSturdy(level, pos.below(), Direction.UP)) {
            return false;
        }
        return level.noCollision(new AABB(Vec3.atLowerCornerOf(pos), Vec3.atLowerCornerOf(pos).add(1, 1.8, 1)));
    }
}
