package at.haha007.edenclient.utils.pathing.segment;

import at.haha007.edenclient.callbacks.PlayerTickCallback;
import at.haha007.edenclient.callbacks.ShouldDoFakeSneakCallback;
import at.haha007.edenclient.utils.PlayerUtils;
import at.haha007.edenclient.utils.tasks.Task;
import at.haha007.edenclient.utils.tasks.TickingTask;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.NonNull;

public class StraightPathSegment extends PathSegment {
    private static int fakeSneak = -1;

    static {
        ShouldDoFakeSneakCallback.EVENT.register(() -> fakeSneak > 0, StraightPathSegment.class);
        PlayerTickCallback.EVENT.register(player ->
                fakeSneak = fakeSneak > 0 ? fakeSneak - 1 : fakeSneak, StraightPathSegment.class);
    }

    public StraightPathSegment(Vec3 from, Vec3 to) {
        super(from, to);
    }

    @Override
    @NonNull
    public Task follower() {
        return new TickingTask(() -> {
            fakeSneak = 2;
            return !PlayerUtils.walkTowards(to());
        });
    }
}
