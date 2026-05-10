package at.haha007.edenclient.utils.pathing;

import at.haha007.edenclient.utils.pathing.optimization.SegmentCombiner;
import at.haha007.edenclient.utils.pathing.segment.MasterPathSegment;
import at.haha007.edenclient.utils.pathing.segment.PathSegment;
import at.haha007.edenclient.utils.pathing.segmentcalculator.MasterSegmentCalculator;
import at.haha007.edenclient.utils.pathing.segmentcalculator.SegmentCalculator;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

import java.util.*;
import java.util.function.Consumer;

public class PathFinder {
    private static final int MAX_NODES = 100_000;
    private static final double REACH_DISTANCE = 1.0;
    private static final double REACH_DISTANCE_SQUARED = REACH_DISTANCE * REACH_DISTANCE;
    private static final double POSITION_KEY_SCALE = 1_000.0;
    private static final int UNLOADED_CHUNK_GUARD_RADIUS = 1;
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
        return new PathSearch(start, target, exact, null);
    }

    public PathSearch startSearch(Vec3 start, Vec3 target, boolean exact, Consumer<PathSegment> callback) {
        return new PathSearch(start, target, exact, callback);
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
        private final Set<PathNode> chunkBlockedNodes = Collections.newSetFromMap(new IdentityHashMap<>());

        /**
         * Segments from previous resets that are now fixed and prepended to every returned path.
         * This prefix is kept incrementally optimized as new committed segments are appended.
         */
        private final List<PathSegment> committedSegments = new ArrayList<>();
        /**
         * Segments are only finalized (passed to segmentConsumer) once they are committed and optimized
         * and can't change anymore as part of the committed prefix.
         */
        private final Consumer<PathSegment> segmentConsumer;
        private int consumedSegmentsCount;
        private boolean doneSegmentsConsumed;

        private PathNode bestNode;
        private double bestDistanceSquared;
        @Getter
        private SearchStatus status;

        private PathSearch(Vec3 start, Vec3 target, boolean exact, Consumer<PathSegment> finalizedSegmentConsumer) {
            this.target = target;
            this.exact = exact;
            this.segmentConsumer = finalizedSegmentConsumer == null ? s -> {
            } : finalizedSegmentConsumer;
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

        public void advance() {
            advance(DEFAULT_NODE_BUDGET_PER_TICK);
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
                consumeRemainingSegmentsIfDone();
                return true;
            }

            // Retry deferred frontier nodes when their surrounding chunks become available.
            reactivateChunkBlockedNodes();
            if (!chunkBlockedNodes.isEmpty()) {
                return false;
            }

            if (openSet.isEmpty()) {
                status = exact ? SearchStatus.FAILED : SearchStatus.FOUND_BEST_EFFORT;
                consumeRemainingSegmentsIfDone();
                return true;
            }

            int processedEntries = 0;
            while (processedEntries < maxNodesToProcess && !openSet.isEmpty()) {
                // When the node budget is exhausted, commit the first half of the current best
                // path and continue searching from the midpoint.
                if (allNodes.size() >= MAX_NODES) {
                    extendCommittedPath();
                    if (openSet.isEmpty()) break;
                }

                processedEntries++;

                OpenSetEntry entry = openSet.poll();
                PathNode current = entry.node;

                // Lazy open-set updates: old entries remain in the queue and are skipped here.
                if (Double.compare(entry.gScoreSnapshot, current.gScore) != 0) {
                    continue;
                }

                // Don't expand near not-yet-loaded chunk borders; retry these nodes later.
                if (isNearUnloadedChunk(current.pos)) {
                    chunkBlockedNodes.add(current);
                    // Pause as soon as we touch an unloaded-chunk frontier.
                    break;
                }

                double currentDistanceSquared = current.pos.distanceToSqr(target);
                if (currentDistanceSquared < REACH_DISTANCE_SQUARED) {
                    bestNode = current;
                    bestDistanceSquared = currentDistanceSquared;
                    status = SearchStatus.FOUND_EXACT;
                    consumeRemainingSegmentsIfDone();
                    return true;
                }

                if (currentDistanceSquared < bestDistanceSquared) {
                    bestDistanceSquared = currentDistanceSquared;
                    bestNode = current;
                }

                for (PathSegment segment : calculator.calculateSegments(current.pos)) {
                    Vec3 neighborPos = segment.to();
                    // Let each segment define its own traversal penalty for better route quality.
                    double tentativeGScore = current.gScore + segment.cost();
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

            if (openSet.isEmpty()) {
                if (!chunkBlockedNodes.isEmpty()) {
                    return false;
                }
                status = exact ? SearchStatus.FAILED : SearchStatus.FOUND_BEST_EFFORT;
            }
            consumeRemainingSegmentsIfDone();
            return status.isDone();
        }

        private void reactivateChunkBlockedNodes() {
            Iterator<PathNode> it = chunkBlockedNodes.iterator();
            while (it.hasNext()) {
                PathNode node = it.next();
                if (!isNearUnloadedChunk(node.pos)) {
                    openSet.add(new OpenSetEntry(node, node.gScore,
                            node.gScore + node.pos.distanceTo(target)));
                    it.remove();
                }
            }
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
            return buildFullPath(bestNode);
        }

        /**
         * Returns the committed prefix that will no longer change across future resets.
         */
        public PathSegment getCommittedPathSoFar() {
            return committedSegments.size() > 1 ? new MasterPathSegment(new ArrayList<>(committedSegments)) : null;
        }

        /**
         * Returns only the currently in-progress suffix from the active search origin to the best node.
         */
        public PathSegment getCalculatedPathSoFar() {
            return buildTailPath(bestNode);
        }

        /**
         * Returns the final result using the same semantics as {@link PathFinder#findPath(Vec3, Vec3, boolean)}.
         */
        public PathSegment getResolvedPath() {
            consumeRemainingSegmentsIfDone();
            return switch (status) {
                case FOUND_EXACT, FOUND_BEST_EFFORT -> buildFullPath(bestNode);
                case RUNNING, FAILED, ALREADY_AT_TARGET -> null;
            };
        }

        private void consumeRemainingSegmentsIfDone() {
            if (doneSegmentsConsumed || !status.isDone()) {
                return;
            }
            doneSegmentsConsumed = true;

            if (status != SearchStatus.FOUND_EXACT && status != SearchStatus.FOUND_BEST_EFFORT) {
                return;
            }

            PathSegment resolvedPath = buildFullPath(bestNode);
            if (!(resolvedPath instanceof MasterPathSegment masterPathSegment)) {
                return;
            }

            List<PathSegment> finalSegments = masterPathSegment.children();
            for (int i = consumedSegmentsCount; i < finalSegments.size(); i++) {
                segmentConsumer.accept(finalSegments.get(i));
            }
            consumedSegmentsCount = finalSegments.size();
        }

        public boolean isDone() {
            return status.isDone();
        }

        /**
         * Combines {@link #committedSegments} with the node-chain path from {@code endNode}
         * to form the complete path, then applies iterative optimization until no further improvements can be found.
         */
        private PathSegment buildFullPath(PathNode endNode) {
            List<PathSegment> tail = collectTailSegments(endNode);

            List<PathSegment> all = new ArrayList<>(committedSegments.size() + tail.size());
            all.addAll(committedSegments);
            all.addAll(tail);
            return buildPathFromSegments(all);
        }

        private PathSegment buildTailPath(PathNode endNode) {
            return buildPathFromSegments(collectTailSegments(endNode));
        }

        private List<PathSegment> collectTailSegments(PathNode endNode) {
            // Tail: walk parent chain of endNode
            List<PathSegment> tail = new ArrayList<>();
            PathNode n = endNode;
            while (n != null && n.segment != null) {
                tail.add(n.segment);
                n = n.parent;
            }
            Collections.reverse(tail);
            return tail;
        }

        private PathSegment buildPathFromSegments(List<PathSegment> segments) {
            if (segments.isEmpty()) {
                return null;
            }

            List<PathSegment> all = new ArrayList<>(segments);
            // Apply iterative optimization until no further improvements can be found.
            int size = all.size();
            while (true) {
                all = optimizePath(all);
                if (all.size() >= size) break;
                size = all.size();
            }
            return new MasterPathSegment(all);
        }

        /**
         * Commits the first half of the current best path, resets the search origin to the
         * midpoint, and discards all nodes that branched off before the midpoint while
         * retaining and adjusting all nodes that are still reachable from the new origin.
         */
        private void extendCommittedPath() {
            // Collect the PathNode chain from root → bestNode
            List<PathNode> nodePath = new ArrayList<>();
            PathNode n = bestNode;
            while (n != null) {
                nodePath.add(n);
                n = n.parent;
            }
            Collections.reverse(nodePath); // [startNode, …, bestNode]

            if (nodePath.size() < 2) {
                // Cannot make any forward progress; give up.
                openSet.clear();
                return;
            }

            int midIdx = nodePath.size() / 2;
            // commit the first 25% of the path to be more conservative about resets
            PathNode midNode = nodePath.get(midIdx);
            double midGScore = midNode.gScore;

            // 1. Append the first-half segments to the committed history, optimizing as we go.
            for (int i = 1; i <= midIdx; i++) {
                PathNode pn = nodePath.get(i);
                if (pn.segment != null) {
                    appendOptimizedSegment(committedSegments, pn.segment);
                    for (int j = consumedSegmentsCount; j < committedSegments.size() - 1; j++) {
                        // the last segment might still be optimized by future commits, so only finalize up to the second-last one
                        segmentConsumer.accept(committedSegments.get(j));
                    }
                    consumedSegmentsCount = Math.max(consumedSegmentsCount, committedSegments.size() - 1);
                }
            }

            // 2. Build the dead-zone set (nodes BEFORE the new origin).
            Set<PathNode> deadZone = Collections.newSetFromMap(new IdentityHashMap<>());
            for (int i = 0; i < midIdx; i++) {
                deadZone.add(nodePath.get(i));
            }

            // 3. Detach the new origin from the committed portion.
            midNode.parent = null;
            midNode.segment = null;
            // gScore will be zeroed by the uniform subtraction below.

            // 4. Seed the keep-cache so ancestor-walks short-circuit quickly.
            Map<PathNode, Boolean> keepCache = new IdentityHashMap<>();
            keepCache.put(midNode, true);
            for (PathNode dead : deadZone) {
                keepCache.put(dead, false);
            }

            // 5. Snapshot which nodes were still in the open set (non-stale entries).
            Set<PathNode> openNodes = Collections.newSetFromMap(new IdentityHashMap<>());
            for (OpenSetEntry entry : openSet) {
                if (Double.compare(entry.gScoreSnapshot, entry.node.gScore) == 0) {
                    openNodes.add(entry.node);
                }
            }
            openSet.clear();

            // 6. Rebuild allNodes: keep only descendants of midNode, adjust gScores.
            Map<PositionKey, PathNode> newAllNodes = new HashMap<>();
            for (Map.Entry<PositionKey, PathNode> entry : allNodes.entrySet()) {
                PathNode node = entry.getValue();
                if (shouldKeep(node, keepCache)) {
                    node.gScore -= midGScore; // midNode itself becomes 0
                    newAllNodes.put(entry.getKey(), node);
                }
            }
            allNodes.clear();
            allNodes.putAll(newAllNodes);

            // 7. Rebuild the open set with surviving open nodes.
            Set<PathNode> survivingNodes = Collections.newSetFromMap(new IdentityHashMap<>());
            survivingNodes.addAll(newAllNodes.values());
            for (PathNode node : openNodes) {
                if (survivingNodes.contains(node)) {
                    openSet.add(new OpenSetEntry(node, node.gScore,
                            node.gScore + node.pos.distanceTo(target)));
                }
            }

            // 8. Keep bestNode consistent; it is always in the second half, but verify.
            if (!survivingNodes.contains(bestNode)) {
                bestNode = midNode;
                bestDistanceSquared = midNode.pos.distanceToSqr(target);
            }
            // bestNode.gScore was already adjusted in step 6.
        }

        /**
         * Walks up {@code node}'s parent chain to determine whether it is a descendant of
         * the midNode (kept=true) or of a dead-zone node (kept=false).  Results are memoised
         * in {@code cache} so each node is visited at most once across all calls.
         */
        private boolean shouldKeep(PathNode node, Map<PathNode, Boolean> cache) {
            List<PathNode> chain = new ArrayList<>();
            PathNode current = node;
            while (!cache.containsKey(current)) {
                chain.add(current);
                if (current.parent == null) {
                    // Orphaned node not connected to any known ancestor – discard.
                    for (PathNode c : chain) cache.put(c, false);
                    return false;
                }
                current = current.parent;
            }
            boolean keep = cache.get(current);
            for (PathNode c : chain) cache.put(c, keep);
            return keep;
        }
    } // end PathSearch

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

    private static boolean isNearUnloadedChunk(Vec3 pos) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return true;
        }

        BlockPos blockPos = BlockPos.containing(pos);
        int chunkX = blockPos.getX() >> 4;
        int chunkZ = blockPos.getZ() >> 4;

        int r = UNLOADED_CHUNK_GUARD_RADIUS;
        // Check only diagonal guard points around the current chunk.
        return isChunkUnavailable(level, chunkX + r, chunkZ - r)   // NE
                || isChunkUnavailable(level, chunkX + r, chunkZ + r) // SE
                || isChunkUnavailable(level, chunkX - r, chunkZ + r) // SW
                || isChunkUnavailable(level, chunkX - r, chunkZ - r); // NW
    }

    private static boolean isChunkUnavailable(ClientLevel level, int chunkX, int chunkZ) {
        // getChunkNow only returns a chunk when it is currently loaded client-side.
        return level.getChunkSource().getChunkNow(chunkX, chunkZ) == null;
    }

    private static void appendOptimizedSegment(List<PathSegment> path, PathSegment segment) {
        if (segment == null) {
            return;
        }

        PathSegment current = segment;
        while (!path.isEmpty()) {
            int lastIndex = path.size() - 1;
            PathSegment combined = SEGMENT_COMBINER.combine(path.get(lastIndex), current);
            if (combined == null) {
                break;
            }
            path.remove(lastIndex);
            current = combined;
        }
        path.add(current);
    }

    private static List<PathSegment> optimizePath(List<PathSegment> path) {
        if (path.size() < 2) {
            return path;
        }

        List<PathSegment> optimized = new ArrayList<>();
        PathSegment current = path.getFirst();
        for (int i = 1; i < path.size(); i++) {
            PathSegment next = path.get(i);
            PathSegment combined = SEGMENT_COMBINER.combine(current, next);
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
