package at.haha007.edenclient.mods;

import at.haha007.edenclient.EdenClient;
import at.haha007.edenclient.Mod;
import at.haha007.edenclient.callbacks.JoinWorldCallback;
import at.haha007.edenclient.utils.ChatColor;
import at.haha007.edenclient.utils.PlayerUtils;
import at.haha007.edenclient.utils.Scheduler;
import at.haha007.edenclient.utils.config.ConfigSubscriber;
import at.haha007.edenclient.utils.config.PerWorldConfig;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;

import static at.haha007.edenclient.command.CommandManager.*;

@Mod
public class StepHeight {
    @ConfigSubscriber("0.6")
    float stepHeight = 0.6f;

    public StepHeight() {
        PerWorldConfig.get().register(this, "stepHeight");
        JoinWorldCallback.EVENT.register(this::onWorldJoin);
        registerCommand();
    }

    private void onWorldJoin() {
        EdenClient.getMod(Scheduler.class).scheduleSyncDelayed(this::update, 1);
    }

    private void registerCommand() {
        LiteralArgumentBuilder<ClientSuggestionProvider> node = literal("estepheight");
        node.then(argument("height", FloatArgumentType.floatArg(0))
                        .executes(c -> {
                            stepHeight = c.getArgument("height", Float.class);
                            PlayerUtils.sendModMessage(ChatColor.GOLD + "Set step height to " + stepHeight);
                            update();
                            return 1;
                        }))
                .executes(c -> {
                    stepHeight = 0.6f;
                    PlayerUtils.sendModMessage(ChatColor.GOLD + "Set step height to 0.6");
                    update();
                    return 1;
                });
        register(node, "StepHeight adjusts the step height of the player.");
    }

    private void update() {
        PlayerUtils.getPlayer().setMaxUpStep(stepHeight);
    }
}
