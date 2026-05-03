package at.haha007.edenclient.utils.pathing.optimization;

import at.haha007.edenclient.utils.pathing.segment.ClimbingPathSegment;
import at.haha007.edenclient.utils.pathing.segment.PathSegment;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;

public class ClimbingSegmentCombiner implements SegmentCombiner {
    @Override
    @Nullable
    public PathSegment combine(@NotNull PathSegment a, @NotNull PathSegment b) {
        if(! (a instanceof ClimbingPathSegment sa) || ! (b instanceof ClimbingPathSegment sb)) return null;
        if (sa.from().subtract(sa.to()).multiply(1,0,1).lengthSqr() > 1e-5) return null;
        if (sb.from().subtract(sb.to()).multiply(1,0,1).lengthSqr() > 1e-5) return null;
        return new ClimbingPathSegment(sa.from(), sb.to());
    }
}
