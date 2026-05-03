package at.haha007.edenclient.utils.pathing;

import at.haha007.edenclient.utils.pathing.optimization.MasterSegmentCombiner;
import at.haha007.edenclient.utils.pathing.optimization.SegmentCombiner;
import at.haha007.edenclient.utils.pathing.optimization.StraightSegmentCombiner;
import at.haha007.edenclient.utils.pathing.segment.MasterPathSegment;
import at.haha007.edenclient.utils.pathing.segment.PathSegment;
import at.haha007.edenclient.utils.pathing.segmentcalculator.MasterSegmentCalculator;
import at.haha007.edenclient.utils.pathing.segmentcalculator.SegmentCalculator;
import lombok.Getter;
import net.minecraft.world.phys.Vec3;

import java.util.*;

public class PathFinder {
    private static final int MAX_NODES = 100_000;
    private static final double REACH_DISTANCE = 1.0;
    private static final double REACH_DISTANCE_SQUARED = REACH_DISTANCE * REACH_DISTANCE;
    private static final double POSITION_KEY_SCALE = 1_000.0;
    public static final int DEFAULT_NODE_BUDGET_PER_TICK = 1_000;
    private static final SegmentCombiner SEGMENT_COMBINER = SegmentCombiner.getDefault();

    private final SegmentCalculator calculator;

    public PathFinder(SegmentCalculator calculator) {
        this.calculator = calculator;
    }

    public static PathFinder createDefault() {
        return new PathFinder(MasterSegmentCalculator.createDefault());
    }

    /**
     * Creates a resumable path search that can be advanced over multiple ticks.
     */
    public PathSearch startSearch(Vec3 start, Vec3 target, boolean exact) {
        return new PathSearch(start, target, exact);
    }

    /**
     * Find a path from start to target
     * This is a blocking compatibility wrapper around {@link #startSearch(Vec3, Vec3, boolean)}.
     *
     * @param start  the starting position
     * @param target the target position
     * @param exact  if it can't find a path, use the nearest possible path
     * @return the path or null if it can't find a path
     */
    public PathSegment findPath(Vec3 start, Vec3 target, boolean exact) {
        return startSearch(start, target, exact).advanceUntilDone();
    }

    @Getter
    public enum SearchStatus {
        RUNNING(false),
        FOUND_EXACT(true),
        FOUND_BEST_EFFORT(true),
        FAILED(true),
        ALREADY_AT_TARGET(true);

        private final boolean done;

        SearchStatus(boolean done) {
            this.done = done;
        }

    }

    public final class PathSearch {
        private final Vec3 target;
        private final boolean exact;
        private final PriorityQueue<OpenSetEntry> openSet = new PriorityQueue<>(Comparator.comparingDouble(n -> n.fScore));
        private final Map<PositionKey, PathNode> allNodes = new HashMap<>(MAX_NODES);

        private PathNode bestNode;
        private double bestDistanceSquared;
        @Getter
        private SearchStatus status;

        private PathSearch(Vec3 start, Vec3 target, boolean exact) {
            this.target = target;
            this.exact = exact;
            PathNode startNode = new PathNode(start, null, null, 0);
            this.bestNode = startNode;
            this.bestDistanceSquared = start.distanceToSqr(target);

            if (bestDistanceSquared < REACH_DISTANCE_SQUARED) {
                status = SearchStatus.ALREADY_AT_TARGET;
                return;
            }

            status = SearchStatus.RUNNING;
            openSet.add(new OpenSetEntry(startNode, 0, start.distanceTo(target)));
            allNodes.put(posKey(start), startNode);
        }

        public boolean advance() {
            return advance(1);
        }

        /**
         * Advances the search by up to {@code maxNodesToProcess} open-set entries.
         *
         * @return true when the search has finished.
         */
        public boolean advance(int maxNodesToProcess) {
            if (maxNodesToProcess <= 0) {
                throw new IllegalArgumentException("maxNodesToProcess has to be >= 1");
            }
            if (status.isDone()) {
                return true;
            }

            int processedEntries = 0;
            while (processedEntries < maxNodesToProcess && !openSet.isEmpty() && allNodes.size() < MAX_NODES) {
                processedEntries++;

                OpenSetEntry entry = openSet.poll();
                PathNode current = entry.node;

                // Lazy open-set updates: old entries remain in the queue and are skipped here.
                if (Double.compare(entry.gScoreSnapshot, current.gScore) != 0) {
                    continue;
                }

                double currentDistanceSquared = current.pos.distanceToSqr(target);
                if (currentDistanceSquared < REACH_DISTANCE_SQUARED) {
                    bestNode = current;
                    bestDistanceSquared = currentDistanceSquared;
                    status = SearchStatus.FOUND_EXACT;
                    return true;
                }

                if (currentDistanceSquared < bestDistanceSquared) {
                    bestDistanceSquared = currentDistanceSquared;
                    bestNode = current;
                }

                for (PathSegment segment : calculator.calculateSegments(current.pos)) {
                    Vec3 neighborPos = segment.to();
                    double tentativeGScore = current.gScore + current.pos.distanceTo(neighborPos);
                    PositionKey neighborKey = posKey(neighborPos);

                    PathNode neighborNode = allNodes.get(neighborKey);
                    if (neighborNode == null) {
                        neighborNode = new PathNode(neighborPos, current, segment, tentativeGScore);
                        allNodes.put(neighborKey, neighborNode);
                        openSet.add(new OpenSetEntry(neighborNode, tentativeGScore,
                                tentativeGScore + neighborPos.distanceTo(target)));
                    } else if (tentativeGScore < neighborNode.gScore) {
                        neighborNode.parent = current;
                        neighborNode.segment = segment;
                        neighborNode.gScore = tentativeGScore;
                        openSet.add(new OpenSetEntry(neighborNode, tentativeGScore,
                                tentativeGScore + neighborPos.distanceTo(target)));
                    }
                }
            }

            if (openSet.isEmpty() || allNodes.size() >= MAX_NODES) {
                status = exact ? SearchStatus.FAILED : SearchStatus.FOUND_BEST_EFFORT;
            }
            return status.isDone();
        }

        public PathSegment advanceUntilDone() {
            do {
                // advance until the search reaches a terminal state
            } while (!advance(MAX_NODES));
            return getResolvedPath();
        }

        /**
         * Returns the currently best-known path, even while the search is still running.
         */
        public PathSegment getBestPathSoFar() {
            return reconstructPath(bestNode);
        }

        /**
         * Returns the final result using the same semantics as {@link PathFinder#findPath(Vec3, Vec3, boolean)}.
         */
        public PathSegment getResolvedPath() {
            return switch (status) {
                case FOUND_EXACT, FOUND_BEST_EFFORT -> reconstructPath(bestNode);
                case RUNNING, FAILED, ALREADY_AT_TARGET -> null;
            };
        }

        public boolean isDone() {
            return status.isDone();
        }
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
        if (path.isEmpty()) return null;
        int size = path.size();
        while(true){
            path = optimizePath(path);
            if(path.size() >= size) break;
            size = path.size();
        }
        return new MasterPathSegment(path);
    }

    private static List<PathSegment> optimizePath(List<PathSegment> path) {
        if (path.size() < 2) {
            return path;
        }

        List<PathSegment> optimized = new ArrayList<>();
        PathSegment current = path.getFirst();
        for (int i = 1; i < path.size(); i++) {
            PathSegment next = path.get(i);
            PathSegment combined = combine(current, next);
            if (combined != null) {
                current = combined;
                continue;
            }
            optimized.add(current);
            current = next;
        }
        optimized.add(current);
        return optimized;
    }

    private static PathSegment combine(PathSegment a, PathSegment b) {
        PathSegment combined = SEGMENT_COMBINER.combine(a, b);
        if (combined != null) {
            return combined;
        }
        return null;
    }

    private static class PathNode {
        final Vec3 pos;
        PathNode parent;
        PathSegment segment;
        double gScore;

        PathNode(Vec3 pos, PathNode parent, PathSegment segment, double gScore) {
            this.pos = pos;
            this.parent = parent;
            this.segment = segment;
            this.gScore = gScore;
        }
    }

    private record OpenSetEntry(PathNode node, double gScoreSnapshot, double fScore) {
    }

    private record PositionKey(long x, long y, long z) {
    }
}
