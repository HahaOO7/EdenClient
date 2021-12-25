package at.haha007.edenclient.mods;

import at.haha007.edenclient.callbacks.GameRenderCallback;
import at.haha007.edenclient.callbacks.JoinWorldCallback;
import at.haha007.edenclient.callbacks.LeaveWorldCallback;
import at.haha007.edenclient.callbacks.PlayerTickCallback;
import at.haha007.edenclient.utils.ChatColor;
import at.haha007.edenclient.utils.PlayerUtils;
import at.haha007.edenclient.utils.RenderUtils;
import at.haha007.edenclient.utils.config.ConfigSubscriber;
import at.haha007.edenclient.utils.config.PerWorldConfig;
import at.haha007.edenclient.utils.config.wrappers.EntityTypeSet;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.client.gl.VertexBuffer;
import net.minecraft.client.network.ClientCommandSource;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.DefaultedRegistry;
import net.minecraft.util.registry.Registry;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static at.haha007.edenclient.command.CommandManager.*;
import static at.haha007.edenclient.utils.PlayerUtils.sendModMessage;

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
    float r, g, b;
    List<Entity> entities = new ArrayList<>();
    private VertexBuffer wireframeBox;
    private VertexBuffer solidBox;

    public EntityEsp() {
        GameRenderCallback.EVENT.register(this::render);
        PlayerTickCallback.EVENT.register(this::tick);
        JoinWorldCallback.EVENT.register(this::build);
        LeaveWorldCallback.EVENT.register(this::destroy);
        PerWorldConfig.get().register(this, "entityEsp");
        registerCommand();
    }

    private void tick(ClientPlayerEntity player) {
        if (!enabled) {
            entities = new ArrayList<>();
            return;
        }
        entities = player.getEntityWorld().getEntitiesByClass(Entity.class,
                player.getBoundingBox().expand(10000, 500, 10000),
                e -> entityTypes.contains(e.getType()) && e.isAlive() && e != player);
    }

    private void build() {
        wireframeBox = new VertexBuffer();
        Box bb = new Box(-0.5, 0, -0.5, 0.5, 1, 0.5);
        RenderUtils.drawOutlinedBox(bb, wireframeBox);

        solidBox = new VertexBuffer();
        RenderUtils.drawSolidBox(bb, solidBox);
    }

    private void destroy() {
        this.entities.clear();
        wireframeBox.close();
        solidBox.close();
    }

    private void registerCommand() {
        LiteralArgumentBuilder<ClientCommandSource> cmd = literal("eentityesp");
        LiteralArgumentBuilder<ClientCommandSource> toggle = literal("toggle");
        toggle.executes(c -> {
            enabled = !enabled;
            sendModMessage(enabled ? "EntityEsp enabled" : "EntityEsp disabled");
            return 1;
        });

        DefaultedRegistry<EntityType<?>> registry = Registry.ENTITY_TYPE;
        for (EntityType<?> type : registry) {
            toggle.then(literal(registry.getId(type).toString().replace("minecraft:", "")).executes(c -> {
                if (!entityTypes.contains(type)) {
                    add(type);
                    sendModMessage("Enabled EntityEsp for EntityType " + type.getUntranslatedName());
                } else {
                    remove(type);
                    sendModMessage("Disabled EntityEsp for EntityType " + type.getUntranslatedName());
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
                    .map(Registry.ENTITY_TYPE::getId)
                    .map(Identifier::toString)
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

    RequiredArgumentBuilder<ClientCommandSource, Integer> arg(String key) {
        return argument(key, IntegerArgumentType.integer(0, 255));
    }

    private int setColor(CommandContext<ClientCommandSource> c) {
        this.r = c.getArgument("r", Integer.class) / 256f;
        this.g = c.getArgument("g", Integer.class) / 256f;
        this.b = c.getArgument("b", Integer.class) / 256f;
        sendModMessage(ChatColor.GOLD + "Color updated.");
        return 1;
    }

    private void remove(EntityType<?> type) {
        entityTypes.remove(type);
    }

    private void add(EntityType<?> type) {
        entityTypes.add(type);
    }

    private void render(MatrixStack matrixStack, VertexConsumerProvider.Immediate vertexConsumerProvider, float tickDelta) {
        if (!enabled) return;
        RenderSystem.setShader(GameRenderer::getPositionShader);
        RenderSystem.setShaderColor(r, g, b, 1);
        RenderSystem.disableDepthTest();
        if (tracer) {
            Vec3d start = RenderUtils.getCameraPos().add(PlayerUtils.getClientLookVec());
            Matrix4f matrix = matrixStack.peek().getPositionMatrix();
            BufferBuilder bb = Tessellator.getInstance().getBuffer();

            bb.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION);
            for (Entity target : entities) {
                Vec3d t = new Vec3d(
                        target.prevX + (target.getX() - target.prevX) * tickDelta,
                        target.prevY + (target.getY() - target.prevY) * tickDelta,
                        target.prevZ + (target.getZ() - target.prevZ) * tickDelta
                );
                bb.vertex(matrix, (float) t.x, (float) t.y, (float) t.z).next();
                bb.vertex(matrix, (float) start.x, (float) start.y, (float) start.z).next();
            }
            bb.end();
            BufferRenderer.draw(bb);
        }
        Runnable drawBoxTask =
                solid ? () -> solidBox.setShader(matrixStack.peek().getPositionMatrix(), RenderSystem.getProjectionMatrix(), RenderSystem.getShader())
                        : () -> wireframeBox.setShader(matrixStack.peek().getPositionMatrix(), RenderSystem.getProjectionMatrix(), RenderSystem.getShader());
        for (Entity target : entities) {
            matrixStack.push();
            matrixStack.translate(
                    target.prevX + (target.getX() - target.prevX) * tickDelta,
                    target.prevY + (target.getY() - target.prevY) * tickDelta,
                    target.prevZ + (target.getZ() - target.prevZ) * tickDelta
            );
            matrixStack.scale(target.getWidth(), target.getHeight(), target.getWidth());
            drawBoxTask.run();
            matrixStack.pop();
        }
    }
}
