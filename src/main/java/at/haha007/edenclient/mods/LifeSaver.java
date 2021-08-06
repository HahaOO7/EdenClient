package at.haha007.edenclient.mods;

import at.haha007.edenclient.callbacks.ConfigLoadCallback;
import at.haha007.edenclient.callbacks.ConfigSaveCallback;
import at.haha007.edenclient.callbacks.PlayerTickCallback;
import at.haha007.edenclient.utils.Scheduler;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientCommandSource;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Formatting;

import static at.haha007.edenclient.command.CommandManager.*;
import static at.haha007.edenclient.utils.PlayerUtils.sendModMessage;

public class LifeSaver {

    int health;
    int height;
    boolean enabled;
    boolean schedulerRunning;

    public LifeSaver() {
        registerCommand();
        PlayerTickCallback.EVENT.register(this::tick);
        ConfigLoadCallback.EVENT.register(this::loadConfig);
        ConfigSaveCallback.EVENT.register(this::saveConfig);
    }

    private void loadConfig(NbtCompound nbtCompound) {
        if (!nbtCompound.contains("lifesaver"))
            return;

        NbtCompound tag = nbtCompound.getCompound("lifesaver");
        if (tag.contains("health"))
            this.health = tag.getInt("health");
        else
            this.health = 1;
        if (tag.contains("height"))
            this.height = tag.getInt("height");
        else
            this.height = -10;
        if (tag.contains("enabled"))
            this.enabled = tag.getBoolean("enabled");
        else
            this.enabled = false;

    }

    private void saveConfig(NbtCompound nbtCompound) {
        NbtCompound tag = new NbtCompound();
        tag.putInt("health", health);
        tag.putInt("height", height);
        tag.putBoolean("enabled", enabled);
        nbtCompound.put("lifesaver", tag);
    }

    private void registerCommand() {
        LiteralArgumentBuilder<ClientCommandSource> node = literal("lifesaver");

        node.then(literal("toggle").executes(c -> {
            enabled = !enabled;
            sendModMessage(new LiteralText(enabled ? "Enabled LifeSaver." : "Disabled LifeSaver.").formatted(Formatting.GOLD));
            return 1;
        }));

        node.then(literal("health").then(argument("health", IntegerArgumentType.integer(0, 20)).executes(c -> {
            this.health = c.getArgument("health", Integer.class);
            sendModMessage(new LiteralText("Set health at which LifeSaves activates to: ").formatted(Formatting.GOLD)
                    .append(new LiteralText("" + health + " (" + (health % 2 == 0 ? health / 2 : health / 2 + ",5") + " full hearts)").formatted(Formatting.AQUA)));
            return 1;
        })));

        node.then(literal("height").then(argument("height", IntegerArgumentType.integer(Integer.MIN_VALUE, 256)).executes(c -> {
            this.height = c.getArgument("height", Integer.class);
            sendModMessage(new LiteralText("Set height at which LifeSaver activates to: ").formatted(Formatting.GOLD)
                    .append(new LiteralText("" + c.getArgument("height", Integer.class)).formatted(Formatting.AQUA)));
            return 1;
        })));

        register(node);
    }

    private void tick(ClientPlayerEntity clientPlayerEntity) {
        if (!enabled || schedulerRunning) return;

        if (clientPlayerEntity.getY() < height || clientPlayerEntity.getHealth() < health) {
            ClientPlayerEntity entityPlayer = MinecraftClient.getInstance().player;
            if (entityPlayer == null) {
                return;
            }

            sendModMessage("Trying to save your life!");

            schedulerRunning = true;
            Scheduler.get().scheduleSyncRepeating(() -> {
                ClientPlayerEntity entity = MinecraftClient.getInstance().player;
                if (entity.getY() > height && entity.getHealth() > health) {
                    sendModMessage("I hope I saved your life!");
                    schedulerRunning = false;
                    return false;
                }
                entityPlayer.sendChatMessage("/farmwelt");
                return true;
            }, 20, 0);
        }
    }
}