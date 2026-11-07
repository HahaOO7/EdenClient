package at.haha007.edenclient.mods;

import at.haha007.edenclient.annotations.Mod;
import at.haha007.edenclient.callbacks.PlayerAttackBlockCallback;
import at.haha007.edenclient.callbacks.PlayerBreakBlockCallback;
import at.haha007.edenclient.mixinterface.IMultiPlayerGameMode;
import at.haha007.edenclient.utils.PlayerUtils;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;

import static at.haha007.edenclient.command.CommandManager.literal;
import static at.haha007.edenclient.command.CommandManager.register;

@Mod
public class FastBreak {

    private boolean enabled = false;

    public FastBreak() {
        LiteralArgumentBuilder<FabricClientCommandSource> node = literal("efastbreak");
        node.then(literal("toggle").executes(_ -> {
            enabled = !enabled;
            PlayerUtils.sendModMessage((enabled ? "FastBreak enabled" : "FastBreak disabled"));
            return 1;
        }));
        register(node, "FastBreak removes the delay between breaking blocks.");
        PlayerAttackBlockCallback.EVENT.register(this::onBreak, getClass());
    }

    private InteractionResult onBreak(LocalPlayer localPlayer, BlockPos blockPos, Direction direction) {
        if (!enabled) {
            return InteractionResult.PASS;
        }
        MultiPlayerGameMode gm = Minecraft.getInstance().gameMode;
        if (!(gm instanceof IMultiPlayerGameMode gameMode)) {
            return InteractionResult.PASS;
        }
        gameMode.edenClient$setBreakDelay(0);
        return InteractionResult.PASS;
    }
}
