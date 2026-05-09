package at.haha007.edenclient.utils.pathing.optimization;

import at.haha007.edenclient.utils.pathing.segment.PathSegment;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;

public interface SegmentCombiner {
    static SegmentCombiner getDefault(){
        return new MasterSegmentCombiner(
                new StraightSegmentCombiner(),
                new SwimmingSegmentCombiner(),
                new ClimbingSegmentCombiner()
        );
    }

    @Nullable
    PathSegment combine(@NotNull PathSegment a, @NotNull PathSegment b);
}
