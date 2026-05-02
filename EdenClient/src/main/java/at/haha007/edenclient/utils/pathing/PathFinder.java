package at.haha007.edenclient.utils.pathing;

import at.haha007.edenclient.utils.pathing.segment.MasterPathSegment;
import at.haha007.edenclient.utils.pathing.segment.PathSegment;
import at.haha007.edenclient.utils.pathing.segmentcalculator.MasterSegmentCalculator;
import at.haha007.edenclient.utils.pathing.segmentcalculator.SegmentCalculator;
import net.minecraft.world.phys.Vec3;

import java.util.*;

public class PathFinder {
    private static final int MAX_NODES = 10_000;
    private static final double REACH_DISTANCE = 1.0;
    private static final double POSITION_KEY_SCALE = 1_000.0;

    private final SegmentCalculator calculator;

    public PathFinder(SegmentCalculator calculator) {
        this.calculator = calculator;
    }

    public static PathFinder createDefault() {
        return new PathFinder(MasterSegmentCalculator.createDefault());
    }

    /**
     * Find a path from start to target
     *
     * @param start  the starting position
     * @param target the target position
     * @param exact  if it can't find a path, use the nearest possible path
     * @return the path or null if it can't find a path
     */
    public PathSegment findPath(Vec3 start, Vec3 target, boolean exact) {
        if (start.distanceTo(target) < REACH_DISTANCE) {
            return null;
        }

        PriorityQueue<PathNode> openSet = new PriorityQueue<>(Comparator.comparingDouble(n -> n.fScore));
        Map<PositionKey, PathNode> allNodes = new HashMap<>();

        PathNode startNode = new PathNode(start, null, null, 0, start.distanceTo(target));
        openSet.add(startNode);
        allNodes.put(posKey(start), startNode);

        PathNode bestNode = startNode;
        double bestDistance = start.distanceTo(target);

        while (!openSet.isEmpty() && allNodes.size() < MAX_NODES) {
            PathNode current = openSet.poll();

            if (current.pos.distanceTo(target) < REACH_DISTANCE) {
                return reconstructPath(current);
            }

            double currentDistance = current.pos.distanceTo(target);
            if (currentDistance < bestDistance) {
                bestDistance = currentDistance;
                bestNode = current;
            }

            for (PathSegment segment : calculator.calculateSegments(current.pos)) {
                Vec3 neighborPos = segment.to();
                double tentativeGScore = current.gScore + current.pos.distanceTo(neighborPos);
                PositionKey neighborKey = posKey(neighborPos);

                PathNode neighborNode = allNodes.get(neighborKey);
                if (neighborNode == null) {
                    neighborNode = new PathNode(neighborPos, current, segment,
                            tentativeGScore, tentativeGScore + neighborPos.distanceTo(target));
                    allNodes.put(neighborKey, neighborNode);
                    openSet.add(neighborNode);
                } else if (tentativeGScore < neighborNode.gScore) {
                    neighborNode.parent = current;
                    neighborNode.segment = segment;
                    neighborNode.gScore = tentativeGScore;
                    neighborNode.fScore = tentativeGScore + neighborPos.distanceTo(target);
                    openSet.remove(neighborNode);
                    openSet.add(neighborNode);
                }
            }
        }

        if (exact) return null;
        return reconstructPath(bestNode);
    }

    /**
     * Creates a map key for a Vec3 position using a fine enough grid to preserve
     * distinct sub-block anchors produced by the segment calculators.
     */
    private static PositionKey posKey(Vec3 pos) {
        long x = Math.round(pos.x * POSITION_KEY_SCALE);
        long y = Math.round(pos.y * POSITION_KEY_SCALE);
        long z = Math.round(pos.z * POSITION_KEY_SCALE);
        return new PositionKey(x, y, z);
    }

    private static PathSegment reconstructPath(PathNode node) {
        List<PathSegment> path = new ArrayList<>();
        while (node != null && node.segment != null) {
            path.add(node.segment);
            node = node.parent;
        }
        Collections.reverse(path);
        if(path.isEmpty()) return null;
        return new MasterPathSegment(path);
    }

    private static class PathNode {
        final Vec3 pos;
        PathNode parent;
        PathSegment segment;
        double gScore;
        double fScore;

        PathNode(Vec3 pos, PathNode parent, PathSegment segment, double gScore, double fScore) {
            this.pos = pos;
            this.parent = parent;
            this.segment = segment;
            this.gScore = gScore;
            this.fScore = fScore;
        }
    }

    private record PositionKey(long x, long y, long z) {
    }
}
