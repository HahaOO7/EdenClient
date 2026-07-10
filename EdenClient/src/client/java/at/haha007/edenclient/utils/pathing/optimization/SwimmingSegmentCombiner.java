package at.haha007.edenclient.utils.pathing.optimization;

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
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;

public class SwimmingSegmentCombiner implements SegmentCombiner {
    private static final double EPSILON = 1e-6;
    private static final double SAMPLE_STEP = 0.2;
    private static final double MAX_LENGTH = 16;

    @Override
    public @Nullable PathSegment combine(@NotNull PathSegment a, @NotNull PathSegment b) {
        if (!(a instanceof SwimmingPathSegment sa) || !(b instanceof SwimmingPathSegment sb)) {
            return null;
        }
        if(a.from().distanceTo(a.to()) > MAX_LENGTH) {
            return null;
        }
        if (sa.to().distanceToSqr(sb.from()) > EPSILON) {
            return null;
        }

        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return null;
        }

        EntityDimensions dimensions = PlayerUtils.getPlayer().getDimensions(PlayerUtils.getPlayer().getPose());
        if (!hasSwimmableDirectPath(level, sa.from(), sb.to(), dimensions)) {
            return null;
        }

        double costA = sa.cost();
        double costB = sb.cost();
        double lengthA = sa.to().distanceTo(sa.to());
        double lengthB = sb.from().distanceTo(sb.to());
        double newLength = sa.from().distanceTo(sb.to());
        double factor = (lengthA - lengthB) / (newLength);
        double newCost = (costA + costB) / factor;
        return new SwimmingPathSegment(sa.from(), sb.to(), newCost);
    }

    private boolean hasSwimmableDirectPath(ClientLevel level, Vec3 from, Vec3 to, EntityDimensions dimensions) {
        double distance = from.distanceTo(to);
        int steps = Math.max(1, (int) Math.ceil(distance / SAMPLE_STEP));
        for (int i = 0; i <= steps; i++) {
            double progress = i / (double) steps;
            Vec3 sample = from.lerp(to, progress);
            AABB box = getPlayerBox(sample, dimensions);
            if (!level.noCollision(box)) {
                return false;
            }
            if (!hasFluidCollision(level, box)) {
                return false;
            }
        }
        return true;
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
}

