package at.haha007.edenclient.mods;

import at.haha007.edenclient.utils.PlayerUtils;
import at.haha007.edenclient.utils.Scheduler;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import java.util.Random;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;

import static at.haha007.edenclient.command.CommandManager.literal;
import static at.haha007.edenclient.command.CommandManager.register;

public class AntiAfk {

    private BlockPos startPos;
    private final Random random = new Random();

    public AntiAfk() {
        var node = literal("eantiafk");

        node.then(literal("toggle").executes(c -> {
            startPos = PlayerUtils.getPlayer().blockPosition();
            Scheduler.get().scheduleSyncRepeating(this::moveAround, 20 * 60 * 5, 0);
            PlayerUtils.sendModMessage("Start moving around randomly in a 3x3 area, walk away to cancel.");
            return 1;
        }));

        register(node,
                "AntiAfk stops you from getting kicked for being afk. ");
    }

    private boolean moveAround() {
        LocalPlayer player = PlayerUtils.getPlayer();
        BlockPos bp = player.blockPosition();
        if (maxDistance(bp, startPos) > 1) return false;

        BlockPos target = getNewTarget();
        while (target.equals(bp)) {
            target = getNewTarget();
        }

        BlockPos finalTarget = target;

        Scheduler.get().scheduleSyncRepeating(() -> {
            Vec3 pos = player.position();
            Vec3 move = Vec3.atBottomCenterOf(finalTarget).subtract(pos);
            move = move.multiply(1, 0, 1);
            if (move.length() > 3) return false;
            if (move.length() > .2)
                player.move(MoverType.SELF, move.normalize().scale(.2));
            else {
                player.move(MoverType.SELF, move);
                return false;
            }
            return true;
        }, 1, 1);

        return true;
    }

    private BlockPos getNewTarget() {
        int r = random.nextInt(5);
        return switch (r) {
            case 0 -> startPos.offset(0, 0, 0);
            case 1 -> startPos.offset(1, 0, 0);
            case 2 -> startPos.offset(0, 0, 1);
            case 3 -> startPos.offset(-1, 0, 0);
            case 4 -> startPos.offset(0, 0, -1);
            default -> null;
        };
    }

    private int maxDistance(Vec3i a, Vec3i b) {
        int x = Math.abs(a.getX() - b.getX());
        int z = Math.abs(a.getZ() - b.getZ());
        return Math.max(x, z);
    }
}
