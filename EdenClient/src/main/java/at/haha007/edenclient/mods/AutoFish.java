package at.haha007.edenclient.mods;

import at.haha007.edenclient.annotations.Mod;
import at.haha007.edenclient.callbacks.PlayerTickCallback;
import at.haha007.edenclient.mixin.FishingHookMixin;
import at.haha007.edenclient.utils.PlayerUtils;
import at.haha007.edenclient.utils.config.ConfigSubscriber;
import at.haha007.edenclient.utils.config.PerWorldConfig;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.xpple.clientarguments.arguments.CAngleArgument;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.FishingRodItem;

import static at.haha007.edenclient.command.CommandManager.*;

@Mod
public class AutoFish {
    @ConfigSubscriber("0")
    private float turn;
    private boolean enabled;

    public AutoFish() {
        registerCommand();
        PerWorldConfig.get().register(this, "autoFish");
        PlayerTickCallback.EVENT.register(this::tick, getClass());
    }

    private void registerCommand() {
        LiteralArgumentBuilder<FabricClientCommandSource> node = literal("eautofish");
        node.then(literal("toggle").executes(c -> {
            enabled = !enabled;
            PlayerUtils.sendModMessage((enabled ? "AutoFish enabled" : "AutoFish disabled"));
            return 1;
        }));
        node.then(literal("turn").executes(c -> {
                    PlayerUtils.sendModMessage("Turning %.2f after fish".formatted(turn));
                    return 1;
                }).then(argument("turn", CAngleArgument.angle()).executes(c -> {
                    turn = CAngleArgument.getAngle(c, "turn");
                    PlayerUtils.sendModMessage("Turn after fish changed to " + turn);
                    return 1;
                }))
        );
        register(node, "AutoFish .");
    }

    private void tick(LocalPlayer player) {
        if(!enabled) return;
        if (!(player.getMainHandItem().getItem() instanceof FishingRodItem)) return;
        FishingHook fishing = player.fishing;
        if(fishing == null) return;
        if(fishing.getHookedIn() != null || ((FishingHookMixin) fishing).edenclient$isFishBiting()) {
            useRod(player);
        }
    }

    private void useRod(LocalPlayer player) {
        MultiPlayerGameMode interactionManager = Minecraft.getInstance().gameMode;
        if(interactionManager == null) return;
        interactionManager.useItem(player, InteractionHand.MAIN_HAND);
        interactionManager.useItem(player, InteractionHand.MAIN_HAND);
        player.setYRot(player.getYRot() + turn);
    }
}
