package at.haha007.edenclient.utils.pathing;

import at.haha007.edenclient.utils.EdenRenderUtils;
import at.haha007.edenclient.utils.pathing.segment.PathSegment;
import fi.dy.masa.malilib.util.data.Color4f;
import net.minecraft.world.phys.Vec3;
import oshi.util.tuples.Pair;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class PathRenderer {
    public static void renderPath(List<PathSegment> segments, float deltaTime) {
        List<Pair<Vec3, Vec3>> lines = new ArrayList<>();
        for (PathSegment segment : segments) {
            lines.add(new Pair<>(segment.from().add(0, .1, 0), segment.to().add(0, .1, 0)));
        }
        EdenRenderUtils.drawLines(lines, Color4f.fromColor(Color.RED.getRGB()));
    }

}
