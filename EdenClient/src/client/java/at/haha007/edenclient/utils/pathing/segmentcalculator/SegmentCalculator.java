package at.haha007.edenclient.utils.pathing.segmentcalculator;

import at.haha007.edenclient.utils.pathing.segment.PathSegment;
import net.minecraft.world.phys.Vec3;

import java.util.Collection;

public interface SegmentCalculator {
    Collection<PathSegment> calculateSegments(Vec3 from);
}
