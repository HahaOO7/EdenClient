package at.haha007.edenclient.utils.pathing.optimization;

import at.haha007.edenclient.utils.pathing.segment.PathSegment;
import org.jspecify.annotations.NonNull;

public class MasterSegmentCombiner implements SegmentCombiner {
    private final SegmentCombiner[] combiners;

    public MasterSegmentCombiner(SegmentCombiner... combiners) {
        this.combiners = combiners;
    }

    @Override
    public PathSegment combine(@NonNull PathSegment a, @NonNull PathSegment b) {
        for (SegmentCombiner combiner : combiners) {
            PathSegment combined = combiner.combine(a, b);
            if (combined != null) {
                return combined;
            }
        }
        return null;
    }
}
