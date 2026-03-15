package at.haha007.edenclient.utils.pathing;

import net.minecraft.core.Vec3i;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Path {
    private final List<Vec3i> path = new ArrayList<>();

    public Path() {
        this(List.of());
    }

    public Path(List<? extends Vec3i> path) {
        this.path.addAll(path);
    }

    public void add(Vec3i pos) {
        path.add(pos);
    }

    public Path subPath(int start, int end) {
        return new Path(path.subList(start, end));
    }

    public Path subPath(int start) {
        return new Path(path.subList(start, path.size()));
    }

    public void removeFirst() {
        if (path.isEmpty()) return;
        path.removeFirst();
    }

    @Nullable
    public PathBlock getBlock(int index) {
        if (index < 0 || index >= path.size() - 1) return null;
        return new PathBlock(path.get(index), path.get(index + 1));
    }

    public List<Vec3i> getPath() {
        return Collections.unmodifiableList(path);
    }

    /**
     * Returns the number of blocks in the path.
     *
     * @return the number of blocks in the path
     */
    public int length() {
        return path.size() - 1;
    }

    /**
     * Returns the nearest block to the given position on the path, or null if the path is empty.
     *
     * @param pos the position to find the nearest block to
     * @return the nearest block to the given position, or null if the path is empty
     */
    public PathPosition getNearest(Vec3 pos) {
        if (path.isEmpty()) {
            return null;
        }

        double minDistance = Double.MAX_VALUE;
        int nearestIndex = 0;
        double nearestProgress = 0.0;

        // Check each segment of the path
        for (int i = 0; i < path.size(); i++) {
            Vec3 currentPos = Vec3.atBottomCenterOf(path.get(i));

            // For the first point, just check distance to the point itself
            if (i == 0) {
                double distance = pos.distanceToSqr(currentPos);
                if (distance < minDistance) {
                    minDistance = distance;
                    nearestProgress = 0.0;
                }
            } else {
                // For segments, check distance to the line segment
                Vec3 prevPos = Vec3.atBottomCenterOf(path.get(i - 1));
                SegmentResult result = findNearestPointOnSegment(pos, prevPos, currentPos);

                if (result.distanceSquared < minDistance) {
                    minDistance = result.distanceSquared;
                    nearestIndex = i - 1;
                    nearestProgress = result.progress;
                }
            }
        }

        return new PathPosition(nearestIndex, nearestProgress, Math.sqrt(minDistance));
    }

    private SegmentResult findNearestPointOnSegment(Vec3 point, Vec3 segmentStart, Vec3 segmentEnd) {
        Vec3 segmentVector = segmentEnd.subtract(segmentStart);
        Vec3 pointVector = point.subtract(segmentStart);

        double segmentLengthSquared = segmentVector.lengthSqr();

        // If segment is essentially a point, return distance to start point
        if (segmentLengthSquared < 1e-10) {
            return new SegmentResult(0.0, point.distanceToSqr(segmentStart));
        }

        // Calculate projection parameter t (clamped to [0,1])
        double t = pointVector.dot(segmentVector) / segmentLengthSquared;
        t = Math.clamp(t, 0.0, 1.0);

        // Find the nearest point on the segment
        Vec3 nearestPoint = segmentStart.add(segmentVector.scale(t));

        return new SegmentResult(t, point.distanceToSqr(nearestPoint));
    }

    private record SegmentResult(double progress, double distanceSquared) {
    }
}
