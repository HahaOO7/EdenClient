package at.haha007.edenclient.utils.pathing.optimization;

import at.haha007.edenclient.utils.PlayerUtils;
import at.haha007.edenclient.utils.pathing.PathingUtils;
import at.haha007.edenclient.utils.pathing.segment.PathSegment;
import at.haha007.edenclient.utils.pathing.segment.StraightPathSegment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;

public class StraightSegmentCombiner implements SegmentCombiner {
    private static final double EPSILON = 1e-6;
    private static final double SUPPORT_CHECK_DEPTH = 0.05;
    private static final double SAMPLE_STEP = 0.1;

    @Override
    public @Nullable PathSegment combine(@NotNull PathSegment a, @NotNull PathSegment b) {
        if(!(a instanceof StraightPathSegment sa) || !(b instanceof StraightPathSegment sb)) return null;
        if(sa.to().distanceToSqr(sb.from()) > EPSILON) return null;
        if(Math.abs(sa.from().y - sa.to().y) > EPSILON) return null;
        if(Math.abs(sb.from().y - sb.to().y) > EPSILON) return null;
        if(Math.abs(sa.from().y - sb.to().y) > EPSILON) return null;

        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return null;

        EntityDimensions dimensions = PlayerUtils.getPlayer().getDimensions(PlayerUtils.getPlayer().getPose());
        Vec3 from = sa.from();
        Vec3 to = sb.to();
        AABB startBox = getStandingBox(from, dimensions);
        Vec3 movement = to.subtract(from);
        if (!PathingUtils.isCollisionFreeMove(startBox, movement)) return null;
        if (!hasWalkableDirectPath(level, from, to, dimensions)) return null;

        //pre-checks done, check if a straight walkable path between start of a and end of b exists
        return new StraightPathSegment(from, to);
    }

    private boolean hasWalkableDirectPath(ClientLevel level, Vec3 from, Vec3 to, EntityDimensions dimensions) {
        double distance = from.distanceTo(to);
        int steps = Math.max(1, (int) Math.ceil(distance / SAMPLE_STEP));
        for (int i = 0; i <= steps; i++) {
            double progress = i / (double) steps;
            Vec3 sample = from.lerp(to, progress);
            AABB box = getStandingBox(sample, dimensions);
            if (!level.noCollision(box)) return false;
            if (hasFluidCollision(level, box)) return false;
            if (level.noCollision(box.move(0, -SUPPORT_CHECK_DEPTH, 0))) return false;
        }
        return true;
    }

    private AABB getStandingBox(Vec3 pos, EntityDimensions dimensions) {
        double halfWidth = dimensions.width() / 2;
        return new AABB(
                pos.x - halfWidth, pos.y, pos.z - halfWidth,
                pos.x + halfWidth, pos.y + dimensions.height(), pos.z + halfWidth
        );
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
}
