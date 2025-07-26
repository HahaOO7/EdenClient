package at.haha007.edenclient.mods;

import at.haha007.edenclient.annotations.Mod;
import at.haha007.edenclient.callbacks.GameRenderCallback;
import at.haha007.edenclient.callbacks.LeaveWorldCallback;
import at.haha007.edenclient.callbacks.PlayerTickCallback;
import at.haha007.edenclient.utils.EdenRenderUtils;
import at.haha007.edenclient.utils.config.ConfigSubscriber;
import at.haha007.edenclient.utils.config.PerWorldConfig;
import at.haha007.edenclient.utils.config.wrappers.EntityTypeSet;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import fi.dy.masa.malilib.util.data.Color4f;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.DefaultedRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static at.haha007.edenclient.command.CommandManager.*;
import static at.haha007.edenclient.utils.PlayerUtils.sendModMessage;

@Mod
public class EntityEsp {
    @ConfigSubscriber("false")
    private boolean enabled;
    @ConfigSubscriber("player")
    private EntityTypeSet entityTypes = new EntityTypeSet();
    @ConfigSubscriber("false")
    boolean solid;
    @ConfigSubscriber("true")
    boolean tracer;
    @ConfigSubscriber("1")
    float red;
    @ConfigSubscriber("1")
    float green;
    @ConfigSubscriber("1")
    float blue;
    List<Entity> entities = new ArrayList<>();

    public EntityEsp() {
        GameRenderCallback.EVENT.register(this::render, getClass());
        PlayerTickCallback.EVENT.register(this::tick, getClass());
        LeaveWorldCallback.EVENT.register(this::destroy, getClass());
        PerWorldConfig.get().register(this, "entityEsp");

        registerCommand();
    }

    private void tick(LocalPlayer player) {
        if (!enabled) {
            entities = new ArrayList<>();
            return;
        }
        entities = player.clientLevel.getEntitiesOfClass(Entity.class,
                player.getBoundingBox().inflate(10000, 500, 10000),
                e -> entityTypes.contains(e.getType()) && e.isAlive() && e != player);
    }


    private void destroy() {
        this.entities.clear();
    }

    private void registerCommand() {
        LiteralArgumentBuilder<FabricClientCommandSource> cmd = literal("eentityesp");
        LiteralArgumentBuilder<FabricClientCommandSource> toggle = literal("toggle");
        toggle.executes(c -> {
            enabled = !enabled;
            sendModMessage(enabled ? "EntityEsp enabled" : "EntityEsp disabled");
            return 1;
        });

        DefaultedRegistry<EntityType<?>> registry = BuiltInRegistries.ENTITY_TYPE;
        for (EntityType<?> type : registry) {
            toggle.then(literal(registry.getKey(type).toString().replace("minecraft:", "")).executes(c -> {
                if (!entityTypes.contains(type)) {
                    add(type);
                    sendModMessage("Enabled EntityEsp for EntityType " + type.toShortString());
                } else {
                    remove(type);
                    sendModMessage("Disabled EntityEsp for EntityType " + type.toShortString());
                }
                return 1;
            }));
        }

        cmd.then(literal("clear").executes(c -> {
            entityTypes.clear();
            sendModMessage("EntityEsp cleared!");
            return 1;
        }));

        cmd.then(literal("list").executes(c -> {
            String str = entityTypes.stream()
                    .map(BuiltInRegistries.ENTITY_TYPE::getKey)
                    .map(ResourceLocation::toString)
                    .map(s -> s.substring(10))
                    .collect(Collectors.joining(", "));
            sendModMessage(str);
            return 1;
        }));

        cmd.then(literal("solid").executes(c -> {
            solid = !solid;
            sendModMessage(solid ? "EntityEsp solid" : "EntityEsp transparent");
            return 1;
        }));

        cmd.then(literal("tracer").executes(c -> {
            tracer = !tracer;
            sendModMessage(tracer ? "Tracer enabled" : "Tracer disabled");
            return 1;
        }));

        cmd.then(literal("color").then(arg("r").then(arg("g").then(arg("b").executes(this::setColor)))));
        cmd.then(toggle);
        register(cmd,
                "EntityEsp allows for all entities of any specific type(s) to be surrounded by x-ray bounding boxes.",
                "It is also possible to enable tracers and to switch between solid/transparent rendering.");
    }

    RequiredArgumentBuilder<FabricClientCommandSource, Integer> arg(String key) {
        return argument(key, IntegerArgumentType.integer(0, 255));
    }

    private int setColor(CommandContext<FabricClientCommandSource> c) {
        this.red = c.getArgument("r", Integer.class) / 256f;
        this.green = c.getArgument("g", Integer.class) / 256f;
        this.blue = c.getArgument("b", Integer.class) / 256f;
        sendModMessage("Color updated.");
        return 1;
    }

    private void remove(EntityType<?> type) {
        entityTypes.remove(type);
    }

    private void add(EntityType<?> type) {
        entityTypes.add(type);
    }

    private void render(float tickDelta) {
        if (!enabled) return;
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        if (tracer && !entities.isEmpty()) {
            EdenRenderUtils.drawTracers(entities.stream().map(e -> e.getPosition(tickDelta)).toList(),
                    Color4f.fromColor(new Color(red, green, blue).getRGB()));
        }
        for (Entity target : entities) {
            drawOutline(target, tickDelta);
        }
    }


    private void drawOutline(Entity entity, float tickDelta) {
        Vec3 positionDelta = entity.getPosition(tickDelta).subtract(entity.getPosition(1));
        AABB boundingBox = entity.getBoundingBox().move(positionDelta);
        EdenRenderUtils.drawAreaOutline(boundingBox.getMinPosition(),
                boundingBox.getMaxPosition(),
                Color4f.fromColor(new Color(red, green, blue).getRGB()));
    }
}
