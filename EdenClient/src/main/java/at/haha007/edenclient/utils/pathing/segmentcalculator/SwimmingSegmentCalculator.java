package at.haha007.edenclient.utils.pathing.segmentcalculator;

import at.haha007.edenclient.utils.PlayerUtils;
import at.haha007.edenclient.utils.pathing.segment.PathSegment;
import at.haha007.edenclient.utils.pathing.segment.SwimmingPathSegment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SwimmingSegmentCalculator implements SegmentCalculator {
    private static final double TARGET_EPSILON_SQUARED = 1e-6;
    private static final double POSITION_KEY_SCALE = 1_000.0;
    private static final double SWIM_STEP = 1;
    private static final double SAMPLE_STEP = 0.6;

    private static final Vec3[] OFFSETS = new Vec3[]{
            // Horizontal swim movement
            new Vec3(SWIM_STEP, 0, 0),
            new Vec3(-SWIM_STEP, 0, 0),
            new Vec3(0, 0, SWIM_STEP),
            new Vec3(0, 0, -SWIM_STEP),
            // Pure vertical swim movement
            new Vec3(0, SWIM_STEP, 0),
            new Vec3(0, -SWIM_STEP, 0),
            // Step out of water: move horizontally while rising
            new Vec3(SWIM_STEP, SWIM_STEP, 0),
            new Vec3(-SWIM_STEP, SWIM_STEP, 0),
            new Vec3(0, SWIM_STEP, SWIM_STEP),
            new Vec3(0, SWIM_STEP, -SWIM_STEP),
            // Enter water from a block edge: move horizontally while dropping a bit
            new Vec3(SWIM_STEP, -SWIM_STEP, 0),
            new Vec3(-SWIM_STEP, -SWIM_STEP, 0),
            new Vec3(0, -SWIM_STEP, SWIM_STEP),
            new Vec3(0, -SWIM_STEP, -SWIM_STEP)
    };

    @Override
    public Collection<PathSegment> calculateSegments(Vec3 from) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return List.of();
        }

        var player = PlayerUtils.getPlayer();
        EntityDimensions dimensions = player.getDimensions(player.getPose());
        double eyeHeight = player.getEyeHeight(player.getPose());

        List<PathSegment> segments = new ArrayList<>();
        Set<PositionKey> seenTargets = new HashSet<>();
        for (Vec3 offset : OFFSETS) {
            Vec3 target = from.add(offset);
            addTarget(level, dimensions, eyeHeight, from, target, segments, seenTargets);
        }

        return segments;
    }

    private void addTarget(ClientLevel level,
                           EntityDimensions dimensions,
                           double eyeHeight,
                           Vec3 from,
                           Vec3 target,
                           List<PathSegment> segments,
                           Set<PositionKey> seenTargets) {
        if (from.distanceToSqr(target) < TARGET_EPSILON_SQUARED) {
            return;
        }
        boolean swimmableTarget = isSwimmable(level, target, dimensions);
        boolean walkableTarget = isWalkableTarget(level, target, dimensions);
        if (!swimmableTarget && !walkableTarget) {
            return;
        }
        if (!hasSwimmableDirectPath(level, from, target, dimensions)) {
            return;
        }

        PositionKey key = toKey(target);
        if (!seenTargets.add(key)) {
            return;
        }
        double cost = isHeadUnderWater(level, target, eyeHeight) ? 10 : 5;
        segments.add(new SwimmingPathSegment(from, target, cost));
    }

    private boolean hasSwimmableDirectPath(ClientLevel level, Vec3 from, Vec3 to, EntityDimensions dimensions) {
        double distance = from.distanceTo(to);
        int steps = Math.max(1, (int) Math.ceil(distance / SAMPLE_STEP));
        boolean touchesFluid = false;
        for (int i = 0; i <= steps; i++) {
            double progress = i / (double) steps;
            Vec3 sample = from.lerp(to, progress);
            AABB box = getPlayerBox(sample, dimensions);
            if (!level.noBlockCollision(null, box, false)) {
                return false;
            }
            if (hasFluidCollision(level, box)) {
                touchesFluid = true;
            }
        }
        return touchesFluid;
    }

    private boolean isSwimmable(ClientLevel level, Vec3 pos, EntityDimensions dimensions) {
        AABB box = getPlayerBox(pos, dimensions);
        return level.noBlockCollision(null, box, false) && hasFluidCollision(level, box);
    }

    private boolean isWalkableTarget(ClientLevel level, Vec3 pos, EntityDimensions dimensions) {
        AABB box = getPlayerBox(pos, dimensions);
        if (!level.noBlockCollision(null, box, false)) {
            return false;
        }
        if (hasFluidCollision(level, box)) {
            return false;
        }

        // Require supporting collision slightly below the feet so the exit target is standable.
        return !level.noBlockCollision(null, box.move(0, -0.05, 0), false);
    }

    private AABB getPlayerBox(Vec3 pos, EntityDimensions dimensions) {
        double halfWidth = dimensions.width() / 2;
        return new AABB(
                pos.x - halfWidth, pos.y, pos.z - halfWidth,
                pos.x + halfWidth, pos.y + dimensions.height(), pos.z + halfWidth
        ).inflate(0.001, 0, 0.001);
    }

    private boolean hasFluidCollision(ClientLevel level, AABB box) {
        int minBlockX = Mth.floor(box.minX + 1e-7);
        int maxBlockX = Mth.floor(box.maxX - 1e-7);
        int minBlockY = Mth.floor(box.minY + 1e-7);
        int maxBlockY = Mth.floor(box.maxY - 1e-7);
        int minBlockZ = Mth.floor(box.minZ + 1e-7);
        int maxBlockZ = Mth.floor(box.maxZ - 1e-7);

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = minBlockX; x <= maxBlockX; x++) {
            for (int y = minBlockY; y <= maxBlockY; y++) {
                for (int z = minBlockZ; z <= maxBlockZ; z++) {
                    pos.set(x, y, z);
                    if (!level.getFluidState(pos).isEmpty()) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private boolean isHeadUnderWater(ClientLevel level, Vec3 pos, double eyeHeight) {
        BlockPos eyePos = BlockPos.containing(pos.x, pos.y + eyeHeight, pos.z);
        return !level.getFluidState(eyePos).isEmpty();
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
