package at.haha007.edenclient.utils.area;

import at.haha007.edenclient.render.*;
import lombok.Getter;
import lombok.experimental.Accessors;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

@Getter
@Accessors(fluent = true)
public class CylinderAreaRenderer implements Renderer {
    private final LineRenderer line = new LineRenderer();
    private final FaceRenderer face = new FaceRenderer();

    private final List<Line> lines = new ArrayList<>();
    private final List<Quad> faces = new ArrayList<>();

    public CylinderAreaRenderer(Vec3 bottomCenter, double height, double radius, int horizontalSteps, int verticalSteps) {
        Vec3 topCenter = bottomCenter.add(0, height, 0);
        // Generate horizontal layers, keeping the ring vertices to build the faces
        Vec3[][] rings = new Vec3[verticalSteps + 1][];
        for (int i = 0; i <= verticalSteps; i++) {
            double y = bottomCenter.y + (height * i) / verticalSteps;
            Vec3 layerCenter = new Vec3(bottomCenter.x, y, bottomCenter.z);
            rings[i] = generateLayer(layerCenter, horizontalSteps, radius);
        }
        // Generate vertical lines
        for (int i = 0; i < horizontalSteps; i++) {
            Vec3 bottomPoint = rings[0][i];
            Vec3 topPoint = rings[verticalSteps][i];
            lines.add(new Line(bottomPoint, topPoint));
        }
        // Generate curved side faces
        for (int i = 0; i < verticalSteps; i++) {
            for (int j = 0; j < horizontalSteps; j++) {
                int k = (j + 1) % horizontalSteps;
                faces.add(new Quad(rings[i][j], rings[i][k], rings[i + 1][k], rings[i + 1][j]));
            }
        }
        // Generate top and bottom caps (point in the middle connected to the circle)
        Vec3 bottomCenterPoint = new Vec3(bottomCenter.x, bottomCenter.y, bottomCenter.z);
        Vec3 topCenterPoint = new Vec3(topCenter.x, topCenter.y, topCenter.z);
        for (int i = 0; i < horizontalSteps; i++) {
            int k = (i + 1) % horizontalSteps;
            Vec3 bottomPoint = rings[0][i];
            Vec3 topPoint = rings[verticalSteps][i];
            lines.add(new Line(bottomCenterPoint, bottomPoint));
            lines.add(new Line(topCenterPoint, topPoint));
            faces.add(new Quad(bottomCenterPoint, bottomPoint, rings[0][k], bottomCenterPoint));
            faces.add(new Quad(topCenterPoint, rings[verticalSteps][k], topPoint, topCenterPoint));
        }
    }

    private Vec3[] generateLayer(Vec3 center, int steps, double radius) {
        Vec3[] ring = new Vec3[steps];
        for (int i = 0; i < steps; i++) {
            double angle1 = 2 * Math.PI * i / steps;
            double angle2 = 2 * Math.PI * (i + 1) / steps;

            Vec3 point1 = new Vec3(
                    center.x + radius * Math.cos(angle1),
                    center.y,
                    center.z + radius * Math.sin(angle1)
            );

            Vec3 point2 = new Vec3(
                    center.x + radius * Math.cos(angle2),
                    center.y,
                    center.z + radius * Math.sin(angle2)
            );

            lines.add(new Line(point1, point2));
            ring[i] = point1;
        }
        return ring;
    }

    @Override
    public void render() {
        line.render(lines);
        face.render(faces);
    }
}
