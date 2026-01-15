package at.haha007.edenclient.utils;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import fi.dy.masa.malilib.render.RenderContext;
import fi.dy.masa.malilib.render.RenderUtils;
import fi.dy.masa.malilib.util.data.Color4f;
import net.minecraft.world.phys.Vec3;
import oshi.util.tuples.Pair;

import java.awt.*;
import java.util.List;

public class BakedLineRenderer implements AutoCloseable{
    private final MeshData meshData;
    RenderContext ctx;

    public BakedLineRenderer(List<Pair<Vec3, Vec3>> lines, RenderPipeline pipeline, Color color) {
        ctx = new RenderContext(() -> "edenclient:drawBoundingBoxEdges", pipeline);
        BufferBuilder buffer = ctx.getBuilder();
        Color4f color4f = Color4f.fromColor(color.getRGB());
        for (Pair<Vec3, Vec3> line : lines) {
            Vec3 start = line.getA().subtract(RenderUtils.camPos());
            Vec3 end = line.getB().subtract(RenderUtils.camPos());
            buffer.addVertex((float) start.x, (float) start.y, (float) start.z).setColor(color4f.r, color4f.g, color4f.b, color4f.a);
            buffer.addVertex((float) end.x, (float) end.y, (float) end.z).setColor(color4f.r, color4f.g, color4f.b, color4f.a);
        }
        meshData = buffer.build();
    }
    
    public void render(){
        if (meshData != null) {
            ctx.draw(meshData, false, false);
            meshData.close();
        }
    }

    @Override
    public void close() throws Exception {
        meshData.close();
        ctx.close();
    }
}
