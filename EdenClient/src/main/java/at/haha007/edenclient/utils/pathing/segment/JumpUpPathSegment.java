package at.haha007.edenclient.utils.pathing.segment;

import at.haha007.edenclient.utils.tasks.Task;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.NotImplementedException;
import org.jetbrains.annotations.NotNull;

public class JumpUpPathSegment extends PathSegment{
    public JumpUpPathSegment(Vec3 from, Vec3 to) {
        super(from, to);
    }

    @Override
    public @NotNull Task follower() {
        return null;
    }

    @Override
    public boolean isValid() {
        throw new NotImplementedException("JumpUpPathSegment is not implemented yet");
    }
}
