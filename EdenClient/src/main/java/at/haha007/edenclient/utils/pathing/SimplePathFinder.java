package at.haha007.edenclient.utils.pathing;

import at.haha007.edenclient.utils.pathing.calculator.NeighborCandidateCalculator;
import at.haha007.edenclient.utils.pathing.calculator.StepDownNeighborCalculator;
import at.haha007.edenclient.utils.pathing.calculator.StepUpNeighborCalculator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.*;

public class SimplePathFinder {
    private final List<NeighborCandidateCalculator> calculators;

    public SimplePathFinder(List<NeighborCandidateCalculator> calculators) {
        this.calculators = calculators;
    }

    public static SimplePathFinder createDefault() {
        return new SimplePathFinder(List.of(
                new StepDownNeighborCalculator(3),
                new StepUpNeighborCalculator(1)
        ));
    }

    /**
     * Find a path from start to target
     *
     * @param start  the starting position
     * @param target the target position
     * @param exact  if it can't find a path, use the nearest possible path
     * @return the path or null if it can't find a path
     */
    public Path findPath(BlockPos start, BlockPos target, boolean exact) {
        if (start.equals(target)) {
            return new Path(List.of(start, target));
        }

        // A* pathfinding algorithm
        PriorityQueue<PathNode> openSet = new PriorityQueue<>(
                Comparator.comparingDouble(n -> n.fScore)
        );
        Map<BlockPos, PathNode> allNodes = new HashMap<>();

        PathNode startNode = new PathNode(start, null, 0, start.distManhattan(target));
        openSet.add(startNode);
        allNodes.put(start, startNode);

        BlockPos bestNode = start;
        double bestDistance = start.distManhattan(target);

        while (!openSet.isEmpty()) {
            PathNode current = openSet.poll();

            if (current.pos.equals(target)) {
                return reconstructPath(current);
            }

            // Update best node for inexact path finding
            double currentDistance = current.pos.distManhattan(target);
            if (currentDistance < bestDistance) {
                bestDistance = currentDistance;
                bestNode = current.pos;
            }

            // Early exit: if the best possible remaining path is worse than our current best
            if (!exact && openSet.peek() != null && openSet.peek().pos.distManhattan(target) > bestDistance + 200) {
                break;
            }

            // Explore neighbors
            for (NeighborCandidateCalculator calculator : calculators) {
                for (BlockPos neighbor : calculator.getValidNeighbors(current.pos)) {
                    double tentativeGScore = current.gScore + calculateCost(current.pos, neighbor);

                    PathNode neighborNode = allNodes.get(neighbor);
                    if (neighborNode == null) {
                        neighborNode = new PathNode(neighbor, current, tentativeGScore,
                                tentativeGScore + neighbor.distManhattan(target));
                        allNodes.put(neighbor, neighborNode);
                        openSet.add(neighborNode);
                    } else if (tentativeGScore < neighborNode.gScore) {
                        neighborNode.parent = current;
                        neighborNode.gScore = tentativeGScore;
                        neighborNode.fScore = tentativeGScore + neighbor.distManhattan(target);
                        if (!openSet.contains(neighborNode)) {
                            openSet.add(neighborNode);
                        }
                    }
                }
            }
        }

        // If exact path is required and not found, return null
        if (exact) {
            return null;
        }

        // Return path to nearest reachable position
        return reconstructPath(allNodes.get(bestNode));
    }

    private double calculateCost(BlockPos pos, BlockPos neighbor) {
        if (neighbor.getY() > pos.getY()) {
            return 3;
        }
        return 1;
    }

    private Path reconstructPath(PathNode node) {
        List<BlockPos> path = new ArrayList<>();
        while (node != null) {
            path.add(node.pos);
            node = node.parent;
        }
        Collections.reverse(path);
        int pathSize;
        do {
            pathSize = path.size();
            optimizePath(path);
        } while (path.size() != pathSize);
        return new Path(path);
    }

    private static class PathNode {
        final BlockPos pos;
        PathNode parent;
        double gScore; // Cost from start
        double fScore; // Estimated total cost

        PathNode(BlockPos pos, PathNode parent, double gScore, double fScore) {
            this.pos = pos;
            this.parent = parent;
            this.gScore = gScore;
            this.fScore = fScore;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof PathNode other)) return false;
            return pos.equals(other.pos);
        }

        @Override
        public int hashCode() {
            return pos.hashCode();
        }
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
            // Check if all blocks along the line are walkable
            boolean allWalkable = start.getY() == end.getY();

            if (allWalkable) {
                // Get all blocks that would be traversed in a straight line
                Collection<BlockPos> blocksAlongLine = getBlocksAlongLine(Vec3.atBottomCenterOf(start), Vec3.atBottomCenterOf(end));
                for (BlockPos block : blocksAlongLine) {
                    if (!isWalkable(block)) {
                        allWalkable = false;
                        break;
                    }
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

    // returns all blocks a player would collide with walking a straight line
    // a player is 0.6x0.6 blocks wide
    // only blocks at foot height in a horizontal line
    private static Collection<BlockPos> getBlocksAlongLine(Vec3 start, Vec3 end) {
        Set<BlockPos> blocks = new HashSet<>();

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
        if (!level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()) {
            return false;
        }
        if (!level.getBlockState(pos.above()).getCollisionShape(level, pos.above()).isEmpty()) {
            return false;
        }
        return true;
    }
}
