package at.haha007.edenclient.mods;

import at.haha007.edenclient.EdenClient;
import at.haha007.edenclient.annotations.Mod;
import at.haha007.edenclient.utils.PlayerUtils;
import at.haha007.edenclient.utils.Scheduler;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;

import java.util.Random;

import static at.haha007.edenclient.command.CommandManager.literal;
import static at.haha007.edenclient.command.CommandManager.register;

@Mod(dependencies = Scheduler.class)
public class AntiAfk {

    private BlockPos startPos;
    private boolean modeWalk = false;

    public AntiAfk() {
        LiteralArgumentBuilder<FabricClientCommandSource> node = literal("eantiafk");

        node.then(literal("toggle").executes(_ -> {
            startPos = PlayerUtils.getPlayer().blockPosition();
            EdenClient.getMod(Scheduler.class).scheduleSyncRepeating(this::interact, 20 * 60 * 5, 0);
            EdenClient.getMod(Scheduler.class).scheduleSyncRepeating(this::moveAround, 20 * 60 * 5, 0);
            PlayerUtils.sendModMessage("Anti afk active, walk away to cancel.");
            return 1;
        }));

        node.then(literal("mode").executes(_ -> {
            modeWalk = !modeWalk;
            PlayerUtils.sendModMessage("Anti afk mode set to " + (modeWalk ? "walk" : "interact"));
            return 1;
        }));

        register(node, "AntiAfk stops you from getting kicked for being afk. ");
    }

    private boolean interact() {
        if (PlayerUtils.shouldPlayLegit()) {
            PlayerUtils.sendModMessage("Anti afk paused because of PlayLegit.");
            return true;
        }
        LocalPlayer player = PlayerUtils.getPlayer();

        if (startPos.distSqr(player.blockPosition()) > 5) {
            PlayerUtils.sendModMessage("Anti afk canceled.");
            return false;
        }
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (connection == null) {
            return false;
        }
        connection.send(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, player.blockPosition(), Direction.UP));
        return true;
    }

    private final Random random = new Random();

    private boolean moveAround() {
        if (PlayerUtils.shouldPlayLegit()) return false;
        LocalPlayer player = PlayerUtils.getPlayer();
        BlockPos bp = player.blockPosition();
        if (maxDistance(bp, startPos) > 1) return false;

        BlockPos target = getNewTarget();
        while (target.equals(bp)) {
            target = getNewTarget();
        }

        BlockPos finalTarget = target;

        EdenClient.getMod(Scheduler.class).scheduleSyncRepeating(() -> {
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
            default -> throw new IllegalStateException("Unexpected value: " + r);
        };
    }

    private int maxDistance(Vec3i a, Vec3i b) {
        int x = Math.abs(a.getX() - b.getX());
        int z = Math.abs(a.getZ() - b.getZ());
        return Math.max(x, z);
    }
}
