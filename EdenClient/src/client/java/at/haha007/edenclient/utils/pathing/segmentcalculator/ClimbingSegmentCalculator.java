package at.haha007.edenclient.utils.pathing.segmentcalculator;

import at.haha007.edenclient.utils.pathing.segment.ClimbingPathSegment;
import at.haha007.edenclient.utils.pathing.segment.PathSegment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

//ladder/vine
public class ClimbingSegmentCalculator implements SegmentCalculator {
    private static final double EDGE_EPSILON = 1e-3;
    private static final double CENTER_EPSILON = 1e-2;
    private static final double BLOCK_CENTER = 0.5;
    private static final double TARGET_EPSILON_SQUARED = 1e-6;
    private static final double VERTICAL_STEP = 0.5;
    private static final double POSITION_KEY_SCALE = 1_000.0;

    @Override
    public Collection<PathSegment> calculateSegments(Vec3 from) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return List.of();
        }

        List<PathSegment> segments = new ArrayList<>();
        Set<PositionKey> seenTargets = new HashSet<>();

        addLadderCenteringMoves(level, from, segments, seenTargets);
        addVerticalClimbingMoves(level, from, segments, seenTargets);

        return segments;
    }

    private void addLadderCenteringMoves(ClientLevel level, Vec3 from, List<PathSegment> segments, Set<PositionKey> seenTargets) {
        int blockX = Mth.floor(from.x);
        int blockY = Mth.floor(from.y);
        int blockZ = Mth.floor(from.z);

        double localX = from.x - blockX;
        double localZ = from.z - blockZ;

        if (isNearZero(localX)) {
            maybeAddCenterMove(level, from, segments, seenTargets,
                    new BlockPos(blockX - 1, blockY, blockZ),
                    new BlockPos(blockX, blockY, blockZ));
        }
        if (isNearOne(localX)) {
            maybeAddCenterMove(level, from, segments, seenTargets,
                    new BlockPos(blockX, blockY, blockZ),
                    new BlockPos(blockX + 1, blockY, blockZ));
        }
        if (isNearZero(localZ)) {
            maybeAddCenterMove(level, from, segments, seenTargets,
                    new BlockPos(blockX, blockY, blockZ - 1),
                    new BlockPos(blockX, blockY, blockZ));
        }
        if (isNearOne(localZ)) {
            maybeAddCenterMove(level, from, segments, seenTargets,
                    new BlockPos(blockX, blockY, blockZ),
                    new BlockPos(blockX, blockY, blockZ + 1));
        }
    }

    private void maybeAddCenterMove(ClientLevel level,
                                    Vec3 from,
                                    List<PathSegment> segments,
                                    Set<PositionKey> seenTargets,
                                    BlockPos first,
                                    BlockPos second) {
        boolean firstClimbable = isClimbable(level, first);
        boolean secondClimbable = isClimbable(level, second);
        if (firstClimbable == secondClimbable) {
            return;
        }

        BlockPos climbablePos = firstClimbable ? first : second;
        Vec3 target = new Vec3(climbablePos.getX() + 0.5, from.y, climbablePos.getZ() + 0.5);
        addTarget(from, target, segments, seenTargets);
    }

    private void addVerticalClimbingMoves(ClientLevel level, Vec3 from, List<PathSegment> segments, Set<PositionKey> seenTargets) {
        if (!isCenteredOnClimbableBlock(level, from)) {
            return;
        }

        Vec3 up = from.add(0, VERTICAL_STEP, 0);
        if (isClimbable(level, BlockPos.containing(up))) {
            addTarget(from, up, segments, seenTargets);
        }

        Vec3 down = from.add(0, -VERTICAL_STEP, 0);
        if (isClimbable(level, BlockPos.containing(down))) {
            addTarget(from, down, segments, seenTargets);
        }
    }

    private void addTarget(Vec3 from, Vec3 target, List<PathSegment> segments, Set<PositionKey> seenTargets) {
        if (from.distanceToSqr(target) < TARGET_EPSILON_SQUARED) {
            return;
        }
        PositionKey key = toKey(target);
        if (!seenTargets.add(key)) {
            return;
        }
        segments.add(new ClimbingPathSegment(from, target));
    }

    private boolean isCenteredOnClimbableBlock(ClientLevel level, Vec3 pos) {
        BlockPos blockPos = BlockPos.containing(pos);
        if (!isClimbable(level, blockPos)) {
            return false;
        }

        double localX = pos.x - blockPos.getX();
        double localZ = pos.z - blockPos.getZ();
        return Math.abs(localX - BLOCK_CENTER) < CENTER_EPSILON
                && Math.abs(localZ - BLOCK_CENTER) < CENTER_EPSILON;
    }

    private boolean isClimbable(ClientLevel level, BlockPos pos) {
        return level.getBlockState(pos).is(BlockTags.CLIMBABLE);
    }

    private boolean isNearZero(double value) {
        return Math.abs(value) < EDGE_EPSILON;
    }

    private boolean isNearOne(double value) {
        return Math.abs(1 - value) < EDGE_EPSILON;
    }

    private PositionKey toKey(Vec3 pos) {
        long x = Math.round(pos.x * POSITION_KEY_SCALE);
        long y = Math.round(pos.y * POSITION_KEY_SCALE);
        long z = Math.round(pos.z * POSITION_KEY_SCALE);
        return new PositionKey(x, y, z);
    }

    private record PositionKey(long x, long y, long z) {
    }
}
