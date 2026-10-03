package at.haha007.edenclient.mods;

import at.haha007.edenclient.annotations.Mod;
import at.haha007.edenclient.callbacks.PlayerTickCallback;
import at.haha007.edenclient.utils.PlayerUtils;
import at.haha007.edenclient.utils.config.ConfigSubscriber;
import at.haha007.edenclient.utils.config.PerWorldConfig;
import at.haha007.edenclient.utils.config.wrappers.EntityTypeSet;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.DefaultedRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ServerboundAttackPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static at.haha007.edenclient.command.CommandManager.*;
import static at.haha007.edenclient.utils.PlayerUtils.sendModMessage;

@Mod
public class KillAura {

    private boolean enabled = false;
    @ConfigSubscriber
    private final EntityTypeSet entityTypes = new EntityTypeSet();
    @ConfigSubscriber("20")
    private int delay = 1;
    private long counter;

    public KillAura() {
        LiteralArgumentBuilder<FabricClientCommandSource> cmd = literal("ekillaura");
        cmd.then(literal("toggle").executes(_ -> {
            enabled = !enabled;
            PlayerUtils.sendModMessage(enabled ? "KillAura enabled" : "KillAura disabled");
            return 1;
        }));

        DefaultedRegistry<EntityType<?>> registry = BuiltInRegistries.ENTITY_TYPE;
        for (EntityType<?> type : registry) {
            cmd.then(literal("add").then(literal(registry.getKey(type).toString().replace("minecraft:", "")).executes(_ -> {
                if (!entityTypes.contains(type)) {
                    add(type);
                    sendModMessage("Enabled KillAura for EntityType " + type.toShortString());
                } else {
                    sendModMessage(type.toShortString() + " is already enabled");
                }
                return 1;
            })));
        }

        cmd.then(removeCommand());

        cmd.then(literal("list").executes(_ -> {
            String str = entityTypes.stream()
                    .map(BuiltInRegistries.ENTITY_TYPE::getKey)
                    .map(Identifier::getPath)
                    .collect(Collectors.joining(", "));
            sendModMessage(str);
            return 1;
        }));

        cmd.then(literal("delay").then(argument("delay", IntegerArgumentType.integer(1)).executes(c -> {
            delay = c.getArgument("delay", Integer.class);
            sendModMessage("Delay is set to " + delay + " ticks");
            return 1;
        })));
        register(cmd, "Hits all entities around the player.");

        PlayerTickCallback.EVENT.register(this::onTick, getClass());
        PerWorldConfig.get().register(this, "killaura");
    }

    private void onTick(LocalPlayer player) {
        if (!enabled) {
            return;
        }
        if (PlayerUtils.shouldPlayLegit()) {
            return;
        }

        counter++;
        if (counter % delay != 0) {
            return;
        }

        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }

        List<Entity> entities = level.getEntitiesOfClass(Entity.class,
                player.getBoundingBox().inflate(10, 10, 10),
                e -> entityTypes.contains(e.getType()) && e.isAlive() && e != player);

        entities.stream()
                .min(Comparator.comparingDouble(e -> e.distanceTo(player)))
                .filter(e -> player.isWithinAttackRange(player.getMainHandItem(), e.getBoundingBox(), 0))
                .ifPresent(e -> {
                    ClientPacketListener connection = Minecraft.getInstance().getConnection();
                    if (connection == null) {
                        return;
                    }
                    connection.send(new ServerboundAttackPacket(e.getId()));
                });

    }

    private void add(EntityType<?> type) {
        entityTypes.add(type);
    }

    private ArgumentBuilder<FabricClientCommandSource, ?> removeCommand() {
        LiteralArgumentBuilder<FabricClientCommandSource> cmd = literal("remove");
        cmd.then(argument("type", StringArgumentType.word()).suggests((_, builder) -> {
            for (EntityType<?> entity : entityTypes) {
                builder.suggest(BuiltInRegistries.ENTITY_TYPE.getKey(entity).getPath());
            }
            return builder.buildFuture();
        }).executes(context -> {
            String name = context.getArgument("type", String.class);
            Identifier identifier = Identifier.parse(name);
            entityTypes.remove(BuiltInRegistries.ENTITY_TYPE.getValue(identifier));
            PlayerUtils.sendModMessage("Removed " + name);
            return 1;
        }));
        return cmd;
    }

}
