package at.haha007.edenclient.utils.pathing.segment;

import at.haha007.edenclient.utils.PlayerUtils;
import at.haha007.edenclient.utils.tasks.Task;
import at.haha007.edenclient.utils.tasks.TickingTask;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.NonNull;

public class ClimbingPathSegment extends PathSegment {
    private static final double COMPLETE_DISTANCE_SQUARED = 0.05 * 0.05;
    private static final double HORIZONTAL_EPSILON = 1e-4;
    private static final double MAX_VERTICAL_SPEED = 0.1176;

    public ClimbingPathSegment(Vec3 from, Vec3 to) {
        super(from, to);
    }

    @Override
    public @NonNull Task follower() {
        return new TickingTask(this::isDoneTick);
    }

    private boolean isDoneTick() {
        LocalPlayer player = PlayerUtils.getPlayer();
        Vec3 delta = to().subtract(player.position());
        if (delta.lengthSqr() < COMPLETE_DISTANCE_SQUARED) {
            return false;
        }

        double horizontalDistance = Math.hypot(delta.x, delta.z);
        double horizontalSpeed = Math.min(PlayerUtils.getWalkingSpeed(), horizontalDistance);

        double vx = 0;
        double vz = 0;
        if (horizontalDistance > HORIZONTAL_EPSILON) {
            double scale = horizontalSpeed / horizontalDistance;
            vx = delta.x * scale;
            vz = delta.z * scale;
        }

        double vy = Mth.clamp(delta.y, -MAX_VERTICAL_SPEED, MAX_VERTICAL_SPEED);

        player.setSprinting(false);
        player.setDeltaMovement(vx, vy, vz);
        return true;
    }
}

