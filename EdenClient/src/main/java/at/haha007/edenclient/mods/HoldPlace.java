package at.haha007.edenclient.mods;

import at.haha007.edenclient.annotations.Mod;
import at.haha007.edenclient.callbacks.PlayerTickCallback;
import at.haha007.edenclient.command.CommandManager;
import at.haha007.edenclient.utils.PlayerUtils;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.ClipContext;

@Mod
public class HoldPlace {
    private boolean enabled = false;
    public HoldPlace() {
        PlayerTickCallback.EVENT.register(this::tick, getClass());
        LiteralArgumentBuilder<FabricClientCommandSource> cmd = CommandManager.literal("eholdplace").executes(c -> {
            enabled = !enabled;
            PlayerUtils.sendModMessage(enabled ? "Hold place enabled" : "Hold place disabled");
            return 1;
        });
        CommandManager.register(cmd, "Hold place by continuously right-clicking.");
    }

    private void tick(LocalPlayer localPlayer) {
        if (!enabled) return;
        if (PlayerUtils.shouldPlayLegit()) return;

        MultiPlayerGameMode gameMode = Minecraft.getInstance().gameMode;
        if (gameMode == null) return;

        Vec3 eyePos = localPlayer.getEyePosition();
        Vec3 look = localPlayer.getLookAngle();
        double reach = localPlayer.blockInteractionRange();
        Vec3 end = eyePos.add(look.scale(reach));
        HitResult hitResult = localPlayer.level().clip(new ClipContext(
                eyePos,
                end,
                ClipContext.Block.OUTLINE,
                ClipContext.Fluid.NONE,
                localPlayer
        ));
        if (hitResult instanceof BlockHitResult blockHitResult) {
            gameMode.useItemOn(localPlayer, InteractionHand.MAIN_HAND, blockHitResult);
            return;
        }
        gameMode.useItem(localPlayer, InteractionHand.MAIN_HAND);
    }
}
