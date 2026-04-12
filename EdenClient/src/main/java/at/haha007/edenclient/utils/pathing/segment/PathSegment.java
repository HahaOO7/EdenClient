package at.haha007.edenclient.utils.pathing.segment;

import at.haha007.edenclient.utils.PlayerUtils;
import at.haha007.edenclient.utils.tasks.Task;
import net.minecraft.core.Vec3i;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

public abstract class PathSegment {
    protected final Vec3 from;
    protected final Vec3 to;
    protected final PlayerAssumptions playerAssumptions;

    protected PathSegment(Vec3 from, Vec3 to) {
        this.from = from;
        this.to = to;
        playerAssumptions = PlayerAssumptions.create();
    }

    public Vec3 from() {
        return from;
    }

    public Vec3 to() {
        return to;
    }

    /**
     * Follow the segment
     * Has to reach the exact target
     *
     * @return a task that moves the player towards the target.
     */
    @NotNull
    public abstract Task follower();

    /**
     * Checks if there are blocks in the way, non walkable blocks on the ground etc.
     *
     * @return true if the segment is valid and can be followed, false otherwise.
     */
    public boolean isValid() {
        return playerAssumptions.isGood(PlayerUtils.getPlayer());
    }
}
