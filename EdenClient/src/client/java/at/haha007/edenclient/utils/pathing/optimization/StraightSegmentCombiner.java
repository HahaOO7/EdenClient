package at.haha007.edenclient.utils.pathing.optimization;

import at.haha007.edenclient.utils.PlayerUtils;
import at.haha007.edenclient.utils.pathing.segment.PathSegment;
import at.haha007.edenclient.utils.pathing.segment.StraightPathSegment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;

public class StraightSegmentCombiner implements SegmentCombiner {
    private static final double EPSILON = 1e-6;
    private static final double STEPS_PER_BLOCK = 5;
    private static final double MAX_LENGTH = 50;
    private static final double MIN_DOT_FOR_COMBINE = Math.cos(1e-6);

    @Override
    public @Nullable PathSegment combine(@NotNull PathSegment a, @NotNull PathSegment b) {
        if (!(a instanceof StraightPathSegment sa) || !(b instanceof StraightPathSegment sb)) {
            return null;
        }
        if (sa.to().distanceToSqr(sb.from()) > EPSILON) {
            return null;
        }
        if (a.from().distanceTo(a.to()) > MAX_LENGTH) {
            return null;
        }
        if (isSmallTurnAngle(sa, sb)) {
            double costA = sa.cost();
            double costB = sb.cost();
            double lengthA = sa.to().distanceTo(sa.to());
            double lengthB = sb.from().distanceTo(sb.to());
            double newLength = sa.from().distanceTo(sb.to());
            double factor = (lengthA - lengthB) / (newLength);
            double newCost = (costA + costB) / factor;
            return new StraightPathSegment(sa.from(), sb.to(), newCost);
        }

        if (Math.abs(sa.from().y - sa.to().y) > EPSILON) {
            return null;
        }
        if (Math.abs(sb.from().y - sb.to().y) > EPSILON) {
            return null;
        }
        if (Math.abs(sa.from().y - sb.to().y) > EPSILON) {
            return null;
        }

        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return null;
        }

        EntityDimensions dimensions = PlayerUtils.getPlayer().getDimensions(PlayerUtils.getPlayer().getPose());
        Vec3 from = sa.from();
        Vec3 to = sb.to();
        if (!hasWalkableDirectPath(level, from, to, dimensions)) {
            return null;
        }

        //pre-checks done, check if a straight walkable path between start of a and end of b exists
        double costA = sa.cost();
        double costB = sb.cost();
        double lengthA = sa.to().distanceTo(sa.to());
        double lengthB = sb.from().distanceTo(sb.to());
        double newLength = sa.from().distanceTo(sb.to());
        double factor = (lengthA - lengthB) / (newLength);
        double newCost = (costA + costB) / factor;
        return new StraightPathSegment(from, to, newCost);
    }

    private boolean hasWalkableDirectPath(ClientLevel level, Vec3 from, Vec3 to, EntityDimensions dimensions) {
        int stepCount =  Mth.ceil(from.distanceTo(to) * STEPS_PER_BLOCK);
        double inflateSize = 1.5 / STEPS_PER_BLOCK;
        for (int i = 1; i <= stepCount; i++) {
            Vec3 pos = from.lerp(to, (double) i / stepCount);
            AABB bigBox = getStandingBox(pos.add(0, EPSILON, 0), dimensions)
                    .inflate(inflateSize, 0, inflateSize);
            //make sure there are no collisions with big box
            if (!level.noCollision(null, bigBox, true)) {
                return false;
            }

            AABB smallBox = getStandingBox(pos.add(0, -.1, 0), dimensions)
                    .deflate(inflateSize, 0, inflateSize);
            //check for floor with small box
            if (level.noCollision(null, smallBox, false)) {
                return false;
            }
        }
        return true;
    }

    private boolean isSmallTurnAngle(StraightPathSegment a, StraightPathSegment b) {
        Vec3 aDirection = a.to().subtract(a.from()).multiply(1, 0, 1).normalize();
        Vec3 bDirection = b.to().subtract(b.from()).multiply(1, 0, 1).normalize();
        double dot = aDirection.dot(bDirection);
        return dot >= MIN_DOT_FOR_COMBINE;
    }

    private AABB getStandingBox(Vec3 pos, EntityDimensions dimensions) {
        double halfWidth = dimensions.width() / 2;
        return new AABB(
                pos.x - halfWidth, pos.y, pos.z - halfWidth,
                pos.x + halfWidth, pos.y + dimensions.height(), pos.z + halfWidth
        );
    }
}
