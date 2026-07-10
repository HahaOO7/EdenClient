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
        master.addCalculator(new StraightSegmentCalculator(3.5));
        master.addCalculator(new SwimmingSegmentCalculator());
        master.addCalculator(new ClimbingSegmentCalculator());
        return master;
    }

    private void addCalculator(SegmentCalculator calculator) {
        calculators.add(calculator);
    }

    @Override
    public Collection<PathSegment> calculateSegments(Vec3 from) {
        List<PathSegment> segments = new ArrayList<>();
        for (SegmentCalculator calculator : calculators) {
            Collection<PathSegment> add = calculator.calculateSegments(from);
            segments.addAll(add);
            for (PathSegment segment : add) {
                Vec3 f = segment.from();
                if(f.distanceTo(from) > 0.0001) {
                    System.out.println();
                }
            }
        }
        return segments;
    }
}
