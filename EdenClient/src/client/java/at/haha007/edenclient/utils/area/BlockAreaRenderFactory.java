package at.haha007.edenclient.utils.area;

import at.haha007.edenclient.utils.EdenRenderUtils;
import at.haha007.edenclient.utils.SphereTriangulator;
import fi.dy.masa.malilib.util.data.Color4f;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.Vec3;
import oshi.util.tuples.Pair;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class BlockAreaRenderFactory {
    public static Runnable createRenderTask(BlockArea area) {
        switch (area) {
            case CubeArea cubeArea -> {
                Vec3i size = cubeArea.getBox().getLength();
                size = new Vec3i(
                        Math.clamp(size.getX() / 5, 1, 30),
                        Math.clamp(size.getY() / 5, 1, 30),
                        Math.clamp(size.getZ() / 5, 1, 30)
                );
                return new CubeAreaRenderTask(cubeArea, size, Color.CYAN);
            }
            case SphereArea sphereArea -> {
                int detail = (int) Math.clamp(Math.log(sphereArea.getRadius()) * 5, 5, 25);
                return new SphereAreaRenderTask(sphereArea, detail, Color.MAGENTA);
            }
            case CylinderArea cylinderArea -> {
                return new CylinderAreaRenderTask(cylinderArea,
                        (int) Math.clamp(cylinderArea.getRadius() * 3, 8, 20),
                        cylinderArea.getHeight() + 1,
                        Color.ORANGE);
            }
            case SavableBlockArea savableBlockArea -> {
                return createRenderTask(savableBlockArea.getArea());
            }
            default -> throw new IllegalArgumentException("Unknown BlockArea type: " + area.getClass().getName());
        }
    }

    private static class CubeAreaRenderTask implements Runnable {
        private final List<Pair<Vec3, Vec3>> lines = new ArrayList<>();
        private final Color4f color;

        public CubeAreaRenderTask(CubeArea cubeArea, Vec3i steps, Color color) {
            this.color = Color4f.fromColor(color.getRGB());
            BoundingBox box = cubeArea.getBox();
            Vec3 min = new Vec3(
                    box.minX(),
                    box.minY(),
                    box.minZ()
            );
            Vec3 max = new Vec3(
                    box.maxX() + 1d,
                    box.maxY() + 1d,
                    box.maxZ() + 1d
            );
            // Generate spaced outlines along X axis
            for (int i = 0; i <= steps.getX(); i++) {
                double x = min.x + (i * (max.x - min.x)) / steps.getX();
                lines.add(new Pair<>(new Vec3(x, min.y, min.z), new Vec3(x, min.y, max.z)));
                lines.add(new Pair<>(new Vec3(x, max.y, min.z), new Vec3(x, max.y, max.z)));
                lines.add(new Pair<>(new Vec3(x, min.y, min.z), new Vec3(x, max.y, min.z)));
                lines.add(new Pair<>(new Vec3(x, min.y, max.z), new Vec3(x, max.y, max.z)));
            }
            // Generate spaced outlines along Y axis
            for (int i = 0; i <= steps.getY(); i++) {
                double y = min.y + (i * (max.y - min.y)) / steps.getY();
                lines.add(new Pair<>(new Vec3(min.x, y, min.z), new Vec3(max.x, y, min.z)));
                lines.add(new Pair<>(new Vec3(min.x, y, max.z), new Vec3(max.x, y, max.z)));
                lines.add(new Pair<>(new Vec3(min.x, y, min.z), new Vec3(min.x, y, max.z)));
                lines.add(new Pair<>(new Vec3(max.x, y, min.z), new Vec3(max.x, y, max.z)));
            }
            // Generate spaced outlines along Z axis
            for (int i = 0; i <= steps.getZ(); i++) {
                double z = min.z + (i * (max.z - min.z)) / steps.getZ();
                lines.add(new Pair<>(new Vec3(min.x, min.y, z), new Vec3(max.x, min.y, z)));
                lines.add(new Pair<>(new Vec3(min.x, max.y, z), new Vec3(max.x, max.y, z)));
                lines.add(new Pair<>(new Vec3(min.x, min.y, z), new Vec3(min.x, max.y, z)));
                lines.add(new Pair<>(new Vec3(max.x, min.y, z), new Vec3(max.x, max.y, z)));
            }
        }

        @Override
        public void run() {
            EdenRenderUtils.drawLines(lines, color);
        }
    }

    private static class SphereAreaRenderTask implements Runnable {
        private final Color4f color;
        private final List<Pair<Vec3, Vec3>> lines;

        public SphereAreaRenderTask(SphereArea sphereArea, int detail, Color color) {
            this.color = Color4f.fromColor(color.getRGB());
            lines = SphereTriangulator.triangulateSphere(sphereArea.center().getCenter(), sphereArea.getRadius(), detail);
        }

        @Override
        public void run() {
            EdenRenderUtils.drawLines(lines, color);
        }
    }

    private static class CylinderAreaRenderTask implements Runnable {
        private final Color4f color;
        private final List<Pair<Vec3, Vec3>> lines = new ArrayList<>();

        public CylinderAreaRenderTask(CylinderArea cylinderArea, int horizontalSteps, int verticalSteps, Color color) {
            this.color = Color4f.fromColor(color.getRGB());
            Vec3 bottomCenter = cylinderArea.getBottomCenter().getBottomCenter();
            Vec3 topCenter = bottomCenter.add(0, cylinderArea.getHeight(), 0);
            double radius = cylinderArea.getRadius() - .5;
            // Generate horizontal layers
            for (int i = 0; i <= verticalSteps; i++) {
                double y = bottomCenter.y + (double) (cylinderArea.getHeight() * i) / verticalSteps;
                Vec3 layerCenter = new Vec3(bottomCenter.x, y, bottomCenter.z);
                lines.addAll(generateLayer(layerCenter, horizontalSteps, radius));
            }
            // Generate vertical lines
            for (int i = 0; i < horizontalSteps; i++) {
                double angle = 2 * Math.PI * i / horizontalSteps;
                Vec3 bottomPoint = new Vec3(
                        bottomCenter.x + radius * Math.cos(angle),
                        bottomCenter.y,
                        bottomCenter.z + radius * Math.sin(angle)
                );
                Vec3 topPoint = new Vec3(
                        topCenter.x + radius * Math.cos(angle),
                        topCenter.y,
                        topCenter.z + radius * Math.sin(angle)
                );
                lines.add(new Pair<>(bottomPoint, topPoint));
            }
            //generate top and bottom circles (point in the middle connected to the circle)
            Vec3 bottomCenterPoint = new Vec3(bottomCenter.x, bottomCenter.y, bottomCenter.z);
            Vec3 topCenterPoint = new Vec3(topCenter.x, topCenter.y, topCenter.z);
            for (int i = 0; i < horizontalSteps; i++) {
                double angle = 2 * Math.PI * i / horizontalSteps;
                Vec3 bottomPoint = new Vec3(
                        bottomCenter.x + radius * Math.cos(angle),
                        bottomCenter.y,
                        bottomCenter.z + radius * Math.sin(angle)
                );
                Vec3 topPoint = new Vec3(
                        topCenter.x + radius * Math.cos(angle),
                        topCenter.y,
                        topCenter.z + radius * Math.sin(angle)
                );
                lines.add(new Pair<>(bottomCenterPoint, bottomPoint));
                lines.add(new Pair<>(topCenterPoint, topPoint));
            }
        }

        private List<Pair<Vec3, Vec3>> generateLayer(Vec3 center, int steps, double radius) {
            List<Pair<Vec3, Vec3>> layerLines = new ArrayList<>();
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

                layerLines.add(new Pair<>(point1, point2));
            }
            return layerLines;
        }

        @Override
        public void run() {
            EdenRenderUtils.drawLines(lines, color);
        }
    }
}
