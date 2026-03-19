package at.haha007.edenclient.utils.pathing;

import at.haha007.edenclient.utils.EdenRenderUtils;
import at.haha007.edenclient.utils.PlayerUtils;
import fi.dy.masa.malilib.util.data.Color4f;
import net.minecraft.core.Vec3i;
import net.minecraft.world.phys.Vec3;
import oshi.util.tuples.Pair;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class PathRenderer {
    public static void renderPath(Path path, float deltaTime) {
        Vec3 playerPos = PlayerUtils.getPlayer().getPosition(deltaTime);
        List<Vec3i> positions = path.getPath();
        PathPosition nearest = path.getNearest(playerPos);
        double progress = nearest.progress();

        PathBlock currentBlock = path.getBlock(nearest.block());
        List<Pair<Vec3, Vec3>> linesFrom = new ArrayList<>();
        for (int i = 0; i < nearest.block(); i++) {
            PathBlock block = path.getBlock(i);
            if (block == null) continue;
            linesFrom.add(new Pair<>(block.startPos().add(0, .1, 0), block.endPos().add(0, .1, 0)));
        }

        List<Pair<Vec3, Vec3>> linesTo = new ArrayList<>();
        for (int i = nearest.block() + 1; i < positions.size(); i++) {
            PathBlock block = path.getBlock(i);
            if (block == null) continue;
            linesTo.add(new Pair<>(block.startPos().add(0, .1, 0), block.endPos().add(0, .1, 0)));
        }

        if (currentBlock != null) {
            linesFrom.add(new Pair<>(currentBlock.startPos().add(0, .1, 0), currentBlock.pointAlongBlock(progress).add(0, .1, 0)));
            linesTo.add(new Pair<>(currentBlock.pointAlongBlock(progress).add(0, .1, 0), currentBlock.endPos().add(0, .1, 0)));
        }

        EdenRenderUtils.drawLines(linesFrom, Color4f.fromColor(Color.GREEN.getRGB()));
//        for (int i = 0; i <= nearest.block(); i++) {
//            Vec3i block = positions.get(i);
//            Vec3 min = Vec3.atLowerCornerOf(block);
//            Vec3 max = min.add(1, 1, 1);
//            EdenRenderUtils.drawAreaOutline(min, max, Color4f.fromColor(Color.GREEN.getRGB()));
//        }

        EdenRenderUtils.drawLines(linesTo, Color4f.fromColor(Color.RED.getRGB()));
//        for (int i = nearest.block() + 1; i < positions.size(); i++) {
//            Vec3i block = positions.get(i);
//            Vec3 min = Vec3.atLowerCornerOf(block);
//            Vec3 max = min.add(1, 1, 1);
//            EdenRenderUtils.drawAreaOutline(min, max, Color4f.fromColor(Color.RED.getRGB()));
//        }
    }

}
