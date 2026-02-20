package at.haha007.edenclient.mods;

import at.haha007.edenclient.annotations.Mod;
import at.haha007.edenclient.callbacks.StepUpHeightCallback;
import at.haha007.edenclient.utils.PlayerUtils;
import at.haha007.edenclient.utils.config.ConfigSubscriber;
import at.haha007.edenclient.utils.config.PerWorldConfig;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;

import java.util.Optional;

import static at.haha007.edenclient.command.CommandManager.*;

@Mod
public class StepHeight {
    @ConfigSubscriber("NaN")
    private float height = Float.NaN;

    public StepHeight() {
        PerWorldConfig.get().register(this, "stepHeight");
        StepUpHeightCallback.EVENT.register(this::onStepUpHeight, getClass());
        registerCommand();
    }

    private Optional<Float> onStepUpHeight() {
        return Float.isNaN(height) ? Optional.empty() : Optional.of(height);
    }

    private void registerCommand() {
        LiteralArgumentBuilder<FabricClientCommandSource> node = literal("estepheight");
        node.then(argument("height", FloatArgumentType.floatArg(0)).executes(c -> {
            height = c.getArgument("height", Float.class);
            PlayerUtils.sendModMessage("Set step height to " + height);
            return 1;
        })).then(literal("reset").executes(c -> {
            height = Float.NaN;
            PlayerUtils.sendModMessage("Set step height reset");
            return 1;
        })).executes(c -> {
            PlayerUtils.sendModMessage("Current step height is " + height);
            return 1;
        });
        register(node, "StepHeight adjusts the step height of the player.");
    }
}
