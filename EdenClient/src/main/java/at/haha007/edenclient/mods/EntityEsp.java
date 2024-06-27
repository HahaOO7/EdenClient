package at.haha007.edenclient.mods;

import at.haha007.edenclient.annotations.Mod;
import at.haha007.edenclient.callbacks.GameRenderCallback;
import at.haha007.edenclient.callbacks.JoinWorldCallback;
import at.haha007.edenclient.callbacks.LeaveWorldCallback;
import at.haha007.edenclient.callbacks.PlayerTickCallback;
import at.haha007.edenclient.utils.PlayerUtils;
import at.haha007.edenclient.utils.RenderUtils;
import at.haha007.edenclient.utils.config.ConfigSubscriber;
import at.haha007.edenclient.utils.config.PerWorldConfig;
import at.haha007.edenclient.utils.config.wrappers.EntityTypeSet;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.DefaultedRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static at.haha007.edenclient.command.CommandManager.*;
import static at.haha007.edenclient.utils.PlayerUtils.sendModMessage;

@Mod
public class EntityEsp {
    @ConfigSubscriber("false")
    private boolean enabled;
    @ConfigSubscriber("player")
    private EntityTypeSet entityTypes;
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
    private VertexBuffer wireframeBox;
    private VertexBuffer solidBox;

    public EntityEsp() {
        GameRenderCallback.EVENT.register(this::render, getClass());
        PlayerTickCallback.EVENT.register(this::tick, getClass());
        JoinWorldCallback.EVENT.register(this::build, getClass());
        LeaveWorldCallback.EVENT.register(this::destroy, getClass());
        PerWorldConfig.get().register(this, "entityEsp");
        registerCommand();
    }

    private void tick(LocalPlayer player) {
        if (!enabled) {
            entities = new ArrayList<>();
            return;
        }
        entities = player.getCommandSenderWorld().getEntitiesOfClass(Entity.class,
                player.getBoundingBox().inflate(10000, 500, 10000),
                e -> entityTypes.contains(e.getType()) && e.isAlive() && e != player);
    }

    private void build() {
        wireframeBox = new VertexBuffer(VertexBuffer.Usage.STATIC);
        AABB bb = new AABB(-0.5, 0, -0.5, 0.5, 1, 0.5);
        RenderUtils.drawOutlinedBox(bb, wireframeBox);

        solidBox = new VertexBuffer(VertexBuffer.Usage.STATIC);
        RenderUtils.drawSolidBox(bb, solidBox);
    }

    private void destroy() {
        this.entities.clear();
        wireframeBox.close();
        solidBox.close();
    }

    private void registerCommand() {
        LiteralArgumentBuilder<ClientSuggestionProvider> cmd = literal("eentityesp");
        LiteralArgumentBuilder<ClientSuggestionProvider> toggle = literal("toggle");
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

    RequiredArgumentBuilder<ClientSuggestionProvider, Integer> arg(String key) {
        return argument(key, IntegerArgumentType.integer(0, 255));
    }

    private int setColor(CommandContext<ClientSuggestionProvider> c) {
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

    private void render(PoseStack matrixStack, MultiBufferSource.BufferSource vertexConsumerProvider, float tickDelta) {
        if (!enabled) return;
        RenderSystem.setShader(GameRenderer::getPositionShader);
        RenderSystem.setShaderColor(red, green, blue, 1);
        RenderSystem.disableDepthTest();
        if (tracer && !entities.isEmpty()) {
            Vec3 start = RenderUtils.getCameraPos().add(PlayerUtils.getClientLookVec());
            Matrix4f matrix = matrixStack.last().pose();
            BufferBuilder bb = Tesselator.getInstance().begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION);
            for (Entity target : entities) {
                Vec3 t = target.getPosition(tickDelta);
                bb.addVertex(matrix, (float) t.x, (float) t.y, (float) t.z);
                bb.addVertex(matrix, (float) start.x, (float) start.y, (float) start.z);
            }
            BufferUploader.drawWithShader(bb.buildOrThrow());
        }
        Runnable drawBoxTask = solid ? () -> draw(solidBox, matrixStack) : () -> draw(wireframeBox, matrixStack);
        for (Entity target : entities) {
            matrixStack.pushPose();
            matrixStack.translate(
                    target.xo + (target.getX() - target.xo) * tickDelta,
                    target.yo + (target.getY() - target.yo) * tickDelta,
                    target.zo + (target.getZ() - target.zo) * tickDelta
            );
            matrixStack.scale(target.getBbWidth(), target.getBbHeight(), target.getBbWidth());
            drawBoxTask.run();
            matrixStack.popPose();
        }
    }

    private void draw(VertexBuffer solidBox, PoseStack matrixStack) {
        solidBox.bind();
        solidBox.drawWithShader(matrixStack.last().pose(), RenderSystem.getProjectionMatrix(), RenderSystem.getShader());
        VertexBuffer.unbind();
    }

}
