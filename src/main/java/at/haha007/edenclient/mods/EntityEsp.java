package at.haha007.edenclient.mods;

import at.haha007.edenclient.callbacks.ConfigLoadCallback;
import at.haha007.edenclient.callbacks.ConfigSaveCallback;
import at.haha007.edenclient.callbacks.GameRenderCallback;
import at.haha007.edenclient.callbacks.PlayerTickCallback;
import at.haha007.edenclient.command.CommandManager;
import at.haha007.edenclient.utils.PlayerUtils;
import at.haha007.edenclient.utils.RenderUtils;
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
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.DefaultedRegistry;
import net.minecraft.util.registry.Registry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import static at.haha007.edenclient.command.CommandManager.argument;
import static at.haha007.edenclient.command.CommandManager.literal;
import static at.haha007.edenclient.utils.PlayerUtils.sendModMessage;

public class EntityEsp {
    private boolean enabled;
    private EntityType<?>[] entityTypes;
    boolean solid;
    boolean tracer;
    int r, g, b;
    List<Entity> entities = new ArrayList<>();
    private VertexBuffer wireframeBox;
    private VertexBuffer solidBox;

    public EntityEsp() {
        GameRenderCallback.EVENT.register(this::render);
        PlayerTickCallback.EVENT.register(this::tick);
        ConfigLoadCallback.EVENT.register(this::load);
        ConfigSaveCallback.EVENT.register(this::save);
        registerCommand();
    }

    private void tick(ClientPlayerEntity player) {
        if (!enabled) {
            entities = new ArrayList<>();
            return;
        }
        entities = player.getEntityWorld().getEntitiesByClass(Entity.class,
                player.getBoundingBox().expand(10000, 500, 10000),
                e -> Arrays.binarySearch(entityTypes, e.getType(), Comparator.comparing(Object::hashCode)) >= 0);
    }

    private void load(NbtCompound nbtCompound) {
        wireframeBox = new VertexBuffer();
        Box bb = new Box(-0.5, 0, -0.5, 0.5, 1, 0.5);
        RenderUtils.drawOutlinedBox(bb, wireframeBox);

        solidBox = new VertexBuffer();
        RenderUtils.drawSolidBox(bb, solidBox);

        var tag = nbtCompound.getCompound("entityEsp");
        if (tag.isEmpty()) {
            enabled = false;
            solid = false;
            r = g = b = 255;
            entityTypes = new EntityType[0];
            return;
        }
        enabled = tag.getBoolean("enabled");
        solid = tag.getBoolean("solid");
        tracer = tag.getBoolean("tracer");

        r = tag.getInt("r");
        g = tag.getInt("g");
        b = tag.getInt("b");

        NbtList entities = tag.getList("entityTypes", NbtElement.STRING_TYPE);
        DefaultedRegistry<EntityType<?>> registry = Registry.ENTITY_TYPE;
        entityTypes = entities.stream()
                .map(NbtElement::asString)
                .map(Identifier::new)
                .map(registry::get)
                .toList()
                .toArray(new EntityType[0]);

        Arrays.sort(entityTypes, Comparator.comparing(Object::hashCode));
    }

    private void save(NbtCompound nbtCompound) {
        NbtCompound tag = new NbtCompound();
        tag.putBoolean("enabled", enabled);
        tag.putBoolean("solid", solid);
        tag.putBoolean("tracer", tracer);

        tag.putInt("r", r);
        tag.putInt("g", g);
        tag.putInt("b", b);

        NbtList entities = new NbtList();
        for (EntityType<?> type : entityTypes) {
            entities.add(NbtString.of(Registry.ENTITY_TYPE.getId(type).toString()));
        }
        tag.put("entityTypes", entities);

        nbtCompound.put("entityEsp", tag);

        this.entities.clear();
        wireframeBox.close();
        solidBox.close();
    }

    private void registerCommand() {
        LiteralArgumentBuilder<ClientCommandSource> cmd = literal("entityesp");
        LiteralArgumentBuilder<ClientCommandSource> toggle = literal("toggle");
        toggle.executes(c -> {
            enabled = !enabled;
            sendModMessage(enabled ? "EntityEsp enabled" : "EntityEsp disabled");
            return 1;
        });

        DefaultedRegistry<EntityType<?>> registry = Registry.ENTITY_TYPE;
        for (EntityType<?> type : registry) {
            toggle.then(literal(registry.getId(type).toString().replace("minecraft:", "")).executes(c -> {
                if (Arrays.binarySearch(entityTypes, type, Comparator.comparing(Object::hashCode)) < 0) {
                    add(type);
                    sendModMessage("Enabled EntityEsp for EntityType " + type.getUntranslatedName());
                } else {
                    remove(type);
                    sendModMessage("Disabled EntityEsp for EntityType " + type.getUntranslatedName());
                }
                System.out.println(Arrays.toString(entityTypes));
                return 1;
            }));
        }

        cmd.then(literal("clear").executes(c -> {
            entityTypes = new EntityType[0];
            sendModMessage("EntityEsp cleared!");
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
        CommandManager.register(cmd);
    }

    RequiredArgumentBuilder<ClientCommandSource, Integer> arg(String key) {
        return argument(key, IntegerArgumentType.integer(0, 255));
    }

    private int setColor(CommandContext<ClientCommandSource> c) {
        this.r = c.getArgument("r", Integer.class);
        this.g = c.getArgument("g", Integer.class);
        this.b = c.getArgument("b", Integer.class);
        sendModMessage(new LiteralText("Color updated.").formatted(Formatting.GOLD));
        return 1;
    }

    private void remove(EntityType<?> type) {
        List<EntityType<?>> list = new ArrayList<>(List.of(entityTypes));
        list.remove(type);
        entityTypes = list.toArray(new EntityType[0]);
        Arrays.sort(entityTypes, Comparator.comparing(Object::hashCode));
    }

    private void add(EntityType<?> type) {
        List<EntityType<?>> list = new ArrayList<>(List.of(entityTypes));
        list.add(type);
        entityTypes = list.toArray(new EntityType[0]);
        Arrays.sort(entityTypes, Comparator.comparing(Object::hashCode));
    }

    private void render(MatrixStack matrixStack, VertexConsumerProvider.Immediate vertexConsumerProvider, float tickDelta) {
        if (!enabled) return;
        RenderSystem.setShader(GameRenderer::getPositionShader);
        RenderSystem.setShaderColor(r, g, b, 1);
        RenderSystem.disableDepthTest();
        if (tracer) {
            Vec3d start = RenderUtils.getCameraPos().add(PlayerUtils.getClientLookVec());
            Matrix4f matrix = matrixStack.peek().getModel();
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
                solid ? () -> solidBox.setShader(matrixStack.peek().getModel(), RenderSystem.getProjectionMatrix(), RenderSystem.getShader())
                        : () -> wireframeBox.setShader(matrixStack.peek().getModel(), RenderSystem.getProjectionMatrix(), RenderSystem.getShader());
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
