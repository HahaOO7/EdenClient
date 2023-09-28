package at.haha007.edenclient.mods;

import at.haha007.edenclient.EdenClient;
import at.haha007.edenclient.Mod;
import at.haha007.edenclient.callbacks.PlayerTickCallback;
import at.haha007.edenclient.utils.ChatColor;
import at.haha007.edenclient.utils.PlayerUtils;
import at.haha007.edenclient.utils.Scheduler;
import at.haha007.edenclient.utils.config.ConfigSubscriber;
import at.haha007.edenclient.utils.config.PerWorldConfig;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.client.player.LocalPlayer;

import static at.haha007.edenclient.command.CommandManager.*;
import static at.haha007.edenclient.utils.PlayerUtils.sendModMessage;

@Mod
public class LifeSaver {

    @ConfigSubscriber("1")
    int health;
    @ConfigSubscriber("-10")
    int height;
    @ConfigSubscriber("false")
    boolean enabled;
    boolean schedulerRunning;

    public LifeSaver() {
        registerCommand();
        PlayerTickCallback.EVENT.register(this::tick);
        PerWorldConfig.get().register(this, "lifesaver");
    }

    private void registerCommand() {
        LiteralArgumentBuilder<ClientSuggestionProvider> node = literal("elifesaver");

        node.then(literal("toggle").executes(c -> {
            enabled = !enabled;
            sendModMessage(ChatColor.GOLD + (enabled ? "Enabled LifeSaver." : "Disabled LifeSaver."));
            return 1;
        }));

        node.then(literal("health").then(argument("health", IntegerArgumentType.integer(0, 20)).executes(c -> {
            this.health = c.getArgument("health", Integer.class);
            sendModMessage(ChatColor.GOLD + "Set health at which LifeSaves activates to: " + ChatColor.AQUA + health + " (" + (health % 2 == 0 ? health / 2 : health / 2 + ",5") + " full hearts)");
            return 1;
        })));

        node.then(literal("height").then(argument("height", IntegerArgumentType.integer(Integer.MIN_VALUE, 256)).executes(c -> {
            this.height = c.getArgument("height", Integer.class);
            sendModMessage(ChatColor.GOLD + "Set height at which LifeSaver activates to: " + ChatColor.AQUA + c.getArgument("height", Integer.class));
            return 1;
        })));

        register(node,
                "LifeSaver saves your life by teleporting you to a safe position when either your health or your y-coordinate reach below a certain value.");
    }

    private void tick(LocalPlayer clientPlayerEntity) {
        if (!enabled || schedulerRunning) return;

        if (clientPlayerEntity.getY() < height || clientPlayerEntity.getHealth() < health) {
            LocalPlayer entityPlayer = PlayerUtils.getPlayer();

            sendModMessage("Trying to save your life!");

            schedulerRunning = true;
            EdenClient.getMod(Scheduler.class).scheduleSyncRepeating(() -> {
                LocalPlayer entity = PlayerUtils.getPlayer();
                if (entity.getY() > height && entity.getHealth() > health) {
                    sendModMessage("I hope I saved your life!");
                    schedulerRunning = false;
                    return false;
                }
                entityPlayer.connection.sendChat("/spawn");
                return true;
            }, 20, 0);
        }
    }
}