package at.haha007.edenclient.utils;

import net.minecraft.world.phys.Vec3;
import oshi.util.tuples.Pair;

import java.util.*;

public class SphereTriangulator {

    public static List<Pair<Vec3, Vec3>> triangulateSphere(
            Vec3 centerPos,
            double radius,
            int latSteps
    ) {
        int lonSteps = latSteps * 2;

        Vec3[][] vertices = new Vec3[latSteps + 1][lonSteps + 1];

        // Generate vertices
        for (int i = 0; i <= latSteps; i++) {
            double v = (double) i / latSteps;
            double theta = Math.PI * v;

            for (int j = 0; j <= lonSteps; j++) {
                double u = (double) j / lonSteps;
                double phi = 2.0 * Math.PI * u;

                double x = radius * Math.sin(theta) * Math.cos(phi);
                double y = radius * Math.cos(theta);
                double z = radius * Math.sin(theta) * Math.sin(phi);

                vertices[i][j] = new Vec3(
                        centerPos.x + x,
                        centerPos.y + y,
                        centerPos.z + z
                );
            }
        }

        Set<EdgeKey> edgeSet = new HashSet<>();

        // Build triangles and extract edges
        for (int i = 0; i < latSteps; i++) {
            for (int j = 0; j < lonSteps; j++) {
                Vec3 v00 = vertices[i][j];
                Vec3 v01 = vertices[i][j + 1];
                Vec3 v10 = vertices[i + 1][j];
                Vec3 v11 = vertices[i + 1][j + 1];

                // Triangle 1
                addEdge(edgeSet, v00, v10);
                addEdge(edgeSet, v10, v11);
                addEdge(edgeSet, v11, v00);

                // Triangle 2
                addEdge(edgeSet, v00, v11);
                addEdge(edgeSet, v11, v01);
                addEdge(edgeSet, v01, v00);
            }
        }

        // Convert to output list
        List<Pair<Vec3, Vec3>> result = new ArrayList<>();
        for (EdgeKey edge : edgeSet) {
            result.add(new Pair<>(edge.a, edge.b));
        }

        return result;
    }

    private static void addEdge(Set<EdgeKey> set, Vec3 a, Vec3 b) {
        set.add(new EdgeKey(a, b));
    }

    /**
     * Order-independent edge key
     */
    private static class EdgeKey {
        Vec3 a;
        Vec3 b;

        EdgeKey(Vec3 v1, Vec3 v2) {
            if (compare(v1, v2) <= 0) {
                this.a = v1;
                this.b = v2;
            } else {
                this.a = v2;
                this.b = v1;
            }
        }

        private static int compare(Vec3 v1, Vec3 v2) {
            if (v1.x != v2.x) return Double.compare(v1.x, v2.x);
            if (v1.y != v2.y) return Double.compare(v1.y, v2.y);
            return Double.compare(v1.z, v2.z);
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof EdgeKey e)) return false;
            return a.equals(e.a) && b.equals(e.b);
        }

        @Override
        public int hashCode() {
            return Objects.hash(a, b);
        }
    }
}
