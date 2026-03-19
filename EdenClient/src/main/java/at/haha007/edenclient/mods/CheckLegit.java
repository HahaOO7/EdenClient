package at.haha007.edenclient.mods;

import at.haha007.edenclient.annotations.Mod;
import at.haha007.edenclient.callbacks.ConfigLoadedCallback;
import at.haha007.edenclient.command.CommandManager;
import at.haha007.edenclient.utils.PlayerUtils;
import at.haha007.edenclient.utils.config.ConfigSubscriber;
import at.haha007.edenclient.utils.config.PerWorldConfig;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import static at.haha007.edenclient.command.CommandManager.literal;

@Mod
public class CheckLegit {
    @ConfigSubscriber("true")
    private boolean checkSpectator;
    @ConfigSubscriber("true")
    private boolean checkNearbyPlayers;

    public CheckLegit() {
        PerWorldConfig.get().register(this, "checkLegit");
        ConfigLoadedCallback.EVENT.register(this::updateConfig, getClass());
        LiteralArgumentBuilder<FabricClientCommandSource> cmd = literal("echecklegit")
                .executes(c -> {
                    sendStatusMessage(Component.text("Current status: "));
                    return 1;
                }).then(literal("spectator").executes(c -> {
                    PlayerUtils.checkSpectator = !PlayerUtils.checkSpectator;
                    sendStatusMessage(Component.text("Spectator updated: "));
                    return 1;
                })).then(literal("nearby").executes(c -> {
                    PlayerUtils.checkNearbyPlayers = !PlayerUtils.checkNearbyPlayers;
                    sendStatusMessage(Component.text("Nearby players updated: "));
                    return 1;
                }));
        CommandManager.register(cmd);
    }

    private void sendStatusMessage(Component prefix) {
        Component spectatorStatus = Component.text("check spectator",
                PlayerUtils.checkSpectator ? NamedTextColor.GREEN : NamedTextColor.RED);
        Component nearbyStatus = Component.text("check nearby players",
                PlayerUtils.checkNearbyPlayers ? NamedTextColor.GREEN : NamedTextColor.RED);
        Component space = Component.text(" - ");
        Component text = prefix.append(spectatorStatus, space, nearbyStatus);
        PlayerUtils.sendModMessage(text);
    }

    private void updateConfig() {
        PlayerUtils.checkNearbyPlayers = checkNearbyPlayers;
        PlayerUtils.checkSpectator = checkSpectator;
    }
}
