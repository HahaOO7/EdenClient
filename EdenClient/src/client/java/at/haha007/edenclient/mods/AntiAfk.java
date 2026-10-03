package at.haha007.edenclient.mods;

import at.haha007.edenclient.EdenClient;
import at.haha007.edenclient.annotations.Mod;
import at.haha007.edenclient.utils.PlayerUtils;
import at.haha007.edenclient.utils.Scheduler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;

import static at.haha007.edenclient.command.CommandManager.literal;
import static at.haha007.edenclient.command.CommandManager.register;

@Mod(dependencies = Scheduler.class)
public class AntiAfk {

    private BlockPos startPos;

    public AntiAfk() {
        var node = literal("eantiafk");

        node.then(literal("toggle").executes(_ -> {
            startPos = PlayerUtils.getPlayer().blockPosition();
            EdenClient.getMod(Scheduler.class).scheduleSyncRepeating(this::interact, 20 * 60 * 5, 0);
            PlayerUtils.sendModMessage("Anti afk active, walk away to cancel.");
            return 1;
        }));

        register(node, "AntiAfk stops you from getting kicked for being afk. ");
    }

    private boolean interact(){
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
        if (connection == null) {return false;}
        connection.send(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, player.blockPosition(),Direction.UP));
        return true;
    }
}
