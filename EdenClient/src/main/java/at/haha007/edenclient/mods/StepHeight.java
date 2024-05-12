package at.haha007.edenclient.mods;

import at.haha007.edenclient.annotations.Mod;
import at.haha007.edenclient.callbacks.ConfigLoadedCallback;
import at.haha007.edenclient.utils.PlayerUtils;
import at.haha007.edenclient.utils.config.ConfigSubscriber;
import at.haha007.edenclient.utils.config.PerWorldConfig;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.netty.util.DefaultAttributeMap;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.core.Registry;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;

import java.util.Objects;

import static at.haha007.edenclient.command.CommandManager.*;

@Mod
public class StepHeight {
    @ConfigSubscriber("0.6")
    private float height = 0.6f;

    public StepHeight() {
        PerWorldConfig.get().register(this, "stepHeight");
        ConfigLoadedCallback.EVENT.register(this::update, getClass());
        registerCommand();
    }

    private void registerCommand() {
        LiteralArgumentBuilder<ClientSuggestionProvider> node = literal("estepheight");
        node.then(argument("height", FloatArgumentType.floatArg(0))
                        .executes(c -> {
                            height = c.getArgument("height", Float.class);
                            PlayerUtils.sendModMessage("Set step height to " + height);
                            update();
                            return 1;
                        }))
                .executes(c -> {
                    height = 0.6f;
                    PlayerUtils.sendModMessage("Set step height to 0.6");
                    update();
                    return 1;
                });
        register(node, "StepHeight adjusts the step height of the player.");
    }

    private void update() {
        Objects.requireNonNull(PlayerUtils.getPlayer().getAttribute(Attributes.STEP_HEIGHT)).setBaseValue(height);
    }
}
