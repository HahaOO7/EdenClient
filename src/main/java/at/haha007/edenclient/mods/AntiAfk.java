package at.haha007.edenclient.mods;

import at.haha007.edenclient.utils.PlayerUtils;
import at.haha007.edenclient.utils.Scheduler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.MovementType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;

import java.util.Random;

import static at.haha007.edenclient.command.CommandManager.literal;
import static at.haha007.edenclient.command.CommandManager.register;

public class AntiAfk {

    private BlockPos startPos;
    private final Random random = new Random();

    public AntiAfk() {
        register(literal("eantiafk").executes(c -> {
            startPos = PlayerUtils.getPlayer().getBlockPos();
            Scheduler.get().scheduleSyncRepeating(this::moveAround, 20 * 60 * 5, 0);
            PlayerUtils.sendModMessage("Start moving around randomly in a 3x3 area, walk away to cancel.");
            return 1;
        }));
    }

    private boolean moveAround() {
        ClientPlayerEntity player = PlayerUtils.getPlayer();
        BlockPos bp = player.getBlockPos();
        if (maxDistance(bp, startPos) > 1) return false;

        BlockPos target = getNewTarget();
        while (target.equals(bp)) {
            target = getNewTarget();
        }

        BlockPos finalTarget = target;

        Scheduler.get().scheduleSyncRepeating(() -> {
            Vec3d pos = player.getPos();
            Vec3d move = Vec3d.ofBottomCenter(finalTarget).subtract(pos);
            if (move.length() > 3) return false;
            if (move.length() > .2)
                player.move(MovementType.SELF, move.normalize().multiply(.2));
            else {
                player.move(MovementType.SELF, move);
                return false;
            }
            return true;
        }, 1, 1);

        return true;
    }

    private BlockPos getNewTarget() {
        int r = random.nextInt(5);
        return switch (r) {
            case 0 -> startPos.add(0, 0, 0);
            case 1 -> startPos.add(1, 0, 0);
            case 2 -> startPos.add(0, 0, 1);
            case 3 -> startPos.add(-1, 0, 0);
            case 4 -> startPos.add(0, 0, -1);
            default -> null;
        };
    }

    private int maxDistance(Vec3i a, Vec3i b) {
        int x = Math.abs(a.getX() - b.getX());
        int z = Math.abs(a.getZ() - b.getZ());
        return Math.max(x, z);
    }
}
