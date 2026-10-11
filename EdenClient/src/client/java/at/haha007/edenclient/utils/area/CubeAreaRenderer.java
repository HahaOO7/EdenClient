package at.haha007.edenclient.utils.area;


import at.haha007.edenclient.render.*;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public class CubeAreaRenderer implements Renderer {
    private final List<Line> lines = new ArrayList<>();
    private final List<Quad> faces = new ArrayList<>();
    private final LineRenderer line = new LineRenderer();
    private final FaceRenderer face = new FaceRenderer();

    public CubeAreaRenderer(AABB box, Vec3i steps) {
        Vec3 min = new Vec3(
                box.minX,
                box.minY,
                box.minZ
        );
        Vec3 max = new Vec3(
                box.maxX + 1d,
                box.maxY + 1d,
                box.maxZ + 1d
        );
        // Generate spaced outlines along X axis
        for (int i = 0; i <= steps.getX(); i++) {
            double x = min.x + (i * (max.x - min.x)) / steps.getX();
            lines.add(new Line(new Vec3(x, min.y, min.z), new Vec3(x, min.y, max.z)));
            lines.add(new Line(new Vec3(x, max.y, min.z), new Vec3(x, max.y, max.z)));
            lines.add(new Line(new Vec3(x, min.y, min.z), new Vec3(x, max.y, min.z)));
            lines.add(new Line(new Vec3(x, min.y, max.z), new Vec3(x, max.y, max.z)));
        }
        // Generate spaced outlines along Y axis
        for (int i = 0; i <= steps.getY(); i++) {
            double y = min.y + (i * (max.y - min.y)) / steps.getY();
            lines.add(new Line(new Vec3(min.x, y, min.z), new Vec3(max.x, y, min.z)));
            lines.add(new Line(new Vec3(min.x, y, max.z), new Vec3(max.x, y, max.z)));
            lines.add(new Line(new Vec3(min.x, y, min.z), new Vec3(min.x, y, max.z)));
            lines.add(new Line(new Vec3(max.x, y, min.z), new Vec3(max.x, y, max.z)));
        }
        // Generate spaced outlines along Z axis
        for (int i = 0; i <= steps.getZ(); i++) {
            double z = min.z + (i * (max.z - min.z)) / steps.getZ();
            lines.add(new Line(new Vec3(min.x, min.y, z), new Vec3(max.x, min.y, z)));
            lines.add(new Line(new Vec3(min.x, max.y, z), new Vec3(max.x, max.y, z)));
            lines.add(new Line(new Vec3(min.x, min.y, z), new Vec3(min.x, max.y, z)));
            lines.add(new Line(new Vec3(max.x, min.y, z), new Vec3(max.x, max.y, z)));
        }
        // Generate faces
        faces.add(new Quad(new Vec3(min.x, min.y, min.z),
                new Vec3(max.x, min.y, min.z),
                new Vec3(max.x, min.y, max.z),
                new Vec3(min.x, min.y, max.z)));
        faces.add(new Quad(new Vec3(min.x, max.y, min.z),
                new Vec3(max.x, max.y, min.z),
                new Vec3(max.x, max.y, max.z),
                new Vec3(min.x, max.y, max.z)));
        faces.add(new Quad(new Vec3(min.x, min.y, min.z),
                new Vec3(max.x, min.y, min.z),
                new Vec3(max.x, max.y, min.z),
                new Vec3(min.x, max.y, min.z)));
        faces.add(new Quad(new Vec3(min.x, min.y, max.z),
                new Vec3(max.x, min.y, max.z),
                new Vec3(max.x, max.y, max.z),
                new Vec3(min.x, max.y, max.z)));
        faces.add(new Quad(new Vec3(min.x, min.y, min.z),
                new Vec3(min.x, min.y, max.z),
                new Vec3(min.x, max.y, max.z),
                new Vec3(min.x, max.y, min.z)));
        faces.add(new Quad(new Vec3(max.x, min.y, min.z),
                new Vec3(max.x, min.y, max.z),
                new Vec3(max.x, max.y, max.z),
                new Vec3(max.x, max.y, min.z)));
    }

    @Override
    public void render() {
        line.render(lines);
        face.render(faces);
    }

    public LineRenderer line() {
        return line;
    }

    public FaceRenderer face() {
        return face;
    }
}
