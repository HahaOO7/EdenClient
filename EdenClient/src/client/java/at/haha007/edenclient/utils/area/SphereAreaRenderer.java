package at.haha007.edenclient.utils.area;

import at.haha007.edenclient.render.*;
import at.haha007.edenclient.utils.SphereTriangulator;
import lombok.Getter;
import lombok.experimental.Accessors;
import net.minecraft.world.phys.Vec3;

import java.util.List;

@Getter
@Accessors(fluent = true)
public class SphereAreaRenderer implements Renderer {
    private final LineRenderer line = new LineRenderer();
    private final FaceRenderer face = new FaceRenderer();

    private final List<Line> lines;
    private final List<Quad> faces;

    public SphereAreaRenderer(Vec3 center, double radius, int detail) {
        lines = SphereTriangulator.triangulateSphere(center, radius, detail);
        faces = SphereTriangulator.triangulateSphereToQuads(center, radius, detail);
    }

    @Override
    public void render() {
        face.render(faces);
        line.render(lines);
    }
}

