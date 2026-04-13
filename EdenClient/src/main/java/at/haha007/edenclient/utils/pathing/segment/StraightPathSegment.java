package at.haha007.edenclient.utils.pathing.segment;

import at.haha007.edenclient.utils.PlayerUtils;
import at.haha007.edenclient.utils.tasks.Task;
import at.haha007.edenclient.utils.tasks.TickingTask;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.NonNull;

public class StraightPathSegment extends PathSegment {

    public StraightPathSegment(Vec3 from, Vec3 to) {
        super(from, to);
    }

    @Override
    @NonNull
    public Task follower() {
        return new TickingTask(() -> !PlayerUtils.walkTowards(to()));
    }
}
