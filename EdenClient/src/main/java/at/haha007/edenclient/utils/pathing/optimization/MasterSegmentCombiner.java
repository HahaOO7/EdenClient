package at.haha007.edenclient.utils.pathing.optimization;

import at.haha007.edenclient.utils.pathing.segment.PathSegment;
import com.mojang.logging.LogUtils;
import net.fabricmc.loader.impl.util.log.Log;
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
                if(a.from().distanceTo(combined.from()) > 1e-5 ||  b.to().distanceTo(combined.to()) > 1e-5) {
                    LogUtils.getLogger().warn("Combiner {} returned a segment with different endpoints than the input segments! This is not allowed and may cause issues in the path optimization. Please report this to the mod author.", combiner.getClass().getSimpleName());
                }
                return combined;
            }
        }
        return null;
    }
}
