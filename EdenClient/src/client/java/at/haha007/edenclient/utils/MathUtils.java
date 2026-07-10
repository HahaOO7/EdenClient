package at.haha007.edenclient.utils;

import net.minecraft.world.phys.Vec3;

public class MathUtils {
    private MathUtils() {
    }

    public static boolean isInteger(String s) {
        try {
            Integer.parseInt(s);
            return true;

        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static Vec3 sdfLine(Vec3 point, Vec3 lineStart, Vec3 lineEnd) {
        Vec3 line = lineEnd.subtract(lineStart);
        Vec3 toPoint = point.subtract(lineStart);

        double lineLengthSquared = line.dot(line);

        // Handle degenerate case (start == end)
        if (lineLengthSquared == 0.0) {
            return lineStart.subtract(point);
        }

        // Project point onto line (parameter t)
        double t = toPoint.dot(line) / lineLengthSquared;

        // Clamp t to segment [0, 1]
        t = Math.clamp(t, 0.0, 1.0);

        // Closest point on the segment
        Vec3 closestPoint = lineStart.add(line.scale(t));

        // Return vector from point to closest point
        return closestPoint.subtract(point);
    }
}
