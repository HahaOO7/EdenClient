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
        register(literal("antiafk").executes(c -> {
            startPos = PlayerUtils.getPlayer().getBlockPos();
            Scheduler.get().scheduleSyncRepeating(() -> {
                ClientPlayerEntity player = PlayerUtils.getPlayer();
                Vec3d pos = player.getPos();
                BlockPos bp = player.getBlockPos();
                if (maxDistance(bp, startPos) > 1) return false;

                Vec3d target = new Vec3d(random.nextDouble(), 0, random.nextDouble());
                target = target.multiply(2).subtract(0.5, 0, 0.5);
                target = target.add(startPos.getX(), startPos.getY(), startPos.getZ());

                Vec3d move = target.subtract(pos);
                if (move.length() > .2)
                    move = move.normalize().multiply(.2);

                player.move(MovementType.SELF, move);
                return true;
            }, 20, 20);
            PlayerUtils.sendModMessage("Start moving around randomly in a 3x3 area, walk away to cancel.");
            return 1;
        }));
    }

    private int maxDistance(Vec3i a, Vec3i b) {
        int x = Math.abs(a.getX() - b.getX());
        int z = Math.abs(a.getZ() - b.getZ());
        return Math.max(x, z);
    }
}
