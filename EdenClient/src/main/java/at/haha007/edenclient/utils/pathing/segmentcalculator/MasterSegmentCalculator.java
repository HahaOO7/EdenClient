package at.haha007.edenclient.utils.pathing.segmentcalculator;

import at.haha007.edenclient.utils.pathing.segment.PathSegment;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class MasterSegmentCalculator implements SegmentCalculator {
    private final List<SegmentCalculator> calculators = new ArrayList<>();

    public static MasterSegmentCalculator createDefault() {
        MasterSegmentCalculator master = new MasterSegmentCalculator();
        master.addCalculator(new StraightSegmentCalculator(3));
        master.addCalculator(new JumpUpSegmentCalculator());
        return master;
    }

    private void addCalculator(SegmentCalculator calculator) {
        calculators.add(calculator);
    }

    @Override
    public Collection<PathSegment> calculateSegments(Vec3 from) {
        List<PathSegment> segments = new ArrayList<>();
        for (SegmentCalculator calculator : calculators) {
            segments.addAll(calculator.calculateSegments(from));
        }
        return segments;
    }
}
