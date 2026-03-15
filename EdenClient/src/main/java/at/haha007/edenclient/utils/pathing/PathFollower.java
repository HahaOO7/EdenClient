package at.haha007.edenclient.utils.pathing;

import at.haha007.edenclient.utils.PlayerUtils;
import com.mojang.logging.LogUtils;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Vec3i;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class PathFollower {
    public static boolean follow(Path path) {
        LocalPlayer player = PlayerUtils.getPlayer();
        PathPosition nearest = path.getNearest(player.position());
        List<Vec3i> positions = path.getPath();
        double progress = nearest.progress();
        PathBlock currentBlock = path.getBlock(nearest.block());
        Vec3 nearestPathPostion = currentBlock.pointAlongBlock(progress);

        if (nearestPathPostion.distanceToSqr(player.position()) > .6) {
            LogUtils.getLogger().info("Distance to path too big");
            return false;
        }

        Vec3 target;
        double distanceToNext = currentBlock.endPos().distanceToSqr(player.position());
        if (distanceToNext < .1) {
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
