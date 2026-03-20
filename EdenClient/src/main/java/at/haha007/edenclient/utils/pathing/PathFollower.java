package at.haha007.edenclient.utils.pathing;

import at.haha007.edenclient.utils.MathUtils;
import at.haha007.edenclient.utils.PlayerUtils;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Vec3i;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class PathFollower {
    public static boolean follow(Path path) {
        LocalPlayer player = PlayerUtils.getPlayer();
        PathPosition nearest = path.getNearest(player.position());
        List<Vec3i> positions = path.getPath();
        PathBlock currentBlock = path.getBlock(nearest.block());
        if(currentBlock == null) {
            return false;
        }
        double horizontalDistance = MathUtils.sdfLine(
                        player.position().multiply(1, 0, 1),
                        currentBlock.startPos().multiply(1, 0, 1),
                        currentBlock.endPos().multiply(1, 0, 1))
                .lengthSqr();
        int startY = currentBlock.start().getY();
        int endY = currentBlock.end().getY();
        int playerY = player.getBlockY();
        int minY = Math.min(startY, endY);
        int maxY = Math.max(startY, endY);
        if (horizontalDistance > .2 || playerY < minY || playerY > maxY) {
            return false;
        }

        Vec3 target;
        double distanceToNext = currentBlock.endPos().distanceToSqr(player.position());
        if (distanceToNext < .001) {
            int nextIndex = nearest.block() + 2;
            if (nextIndex >= positions.size()) {
                return false;
            }
            Vec3i nextTarget = positions.get(nextIndex);
            target = Vec3.atBottomCenterOf(nextTarget);
        } else {
            target = currentBlock.endPos();
        }
        PlayerUtils.walkTowards(target);
        return true;
    }
}
