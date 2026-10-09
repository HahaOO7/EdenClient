package at.haha007.edenclient.utils.pathing;

import at.haha007.edenclient.render.EdenColor;
import at.haha007.edenclient.utils.EdenRenderUtils;
import at.haha007.edenclient.utils.pathing.segment.MasterPathSegment;
import at.haha007.edenclient.utils.pathing.segment.PathSegment;
import net.minecraft.world.phys.Vec3;
import oshi.util.tuples.Pair;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class PathRenderer {
    private final EdenColor color;

    public PathRenderer(Color color) {
        this.color = EdenColor.fromColor(color.getRGB());
    }


    public void renderPath(PathSegment path) {
        if (path == null) return;
        if (path instanceof MasterPathSegment master) {
            renderPath(master.children());
            return;
        }
        renderPath(List.of(path));
    }

    public void renderPath(List<PathSegment> segments) {
        List<Pair<Vec3, Vec3>> lines = new ArrayList<>();
        for (PathSegment segment : segments) {
            lines.add(new Pair<>(segment.from().add(0, .1, 0), segment.to().add(0, .1, 0)));
            EdenRenderUtils.drawAreaOutline(
                    segment.to().add(-0.2, .1, -0.2),
                    segment.to().add(0.2, .5, 0.2),
                    EdenColor.fromColor(Color.BLUE.getRGB()));
        }
        EdenRenderUtils.drawLines(lines, color);
    }

}
