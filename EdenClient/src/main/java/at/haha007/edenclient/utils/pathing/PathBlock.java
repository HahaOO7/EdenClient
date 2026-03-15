package at.haha007.edenclient.utils.pathing;

import net.minecraft.core.Vec3i;
import net.minecraft.world.phys.Vec3;

public record PathBlock(Vec3i start, Vec3i end) {

    /**
     * Returns a point along the block, given a progress between 0 and 1.
     * <\p>
     * The point will be outside the block if the progress is outside [0,1].
     *
     * @param progress a value between 0 and 1, where 0 is the start of the block and 1 is the end of the block
     * @return a point along the block
     */
    public Vec3 pointAlongBlock(double progress) {
        return startPos().lerp(endPos(), progress);
    }

    public Vec3 endPos() {
        return Vec3.atBottomCenterOf(end);
    }

    public Vec3 startPos(){
        return Vec3.atBottomCenterOf(start);
    }

}
