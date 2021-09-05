package at.haha007.edenclient.mods;

import at.haha007.edenclient.callbacks.GameRenderCallback;
import at.haha007.edenclient.callbacks.JoinWorldCallback;
import at.haha007.edenclient.callbacks.LeaveWorldCallback;
import at.haha007.edenclient.callbacks.PlayerTickCallback;
import at.haha007.edenclient.utils.RenderUtils;
import at.haha007.edenclient.utils.config.ConfigSubscriber;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.client.gl.VertexBuffer;
import net.minecraft.client.network.ClientCommandSource;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.ItemEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Box;

import java.util.ArrayList;
import java.util.List;

import static at.haha007.edenclient.command.CommandManager.*;
import static at.haha007.edenclient.utils.PlayerUtils.sendModMessage;

public class ItemEsp {
    @ConfigSubscriber("false")
    boolean enabled = false;
    @ConfigSubscriber("1")
    float r, g, b;
    @ConfigSubscriber("false")
    boolean solid;
    List<ItemEntity> items = new ArrayList<>();
    private VertexBuffer wireframeBox;
    private VertexBuffer solidBox;


    public ItemEsp() {
        registerCommand();
        LeaveWorldCallback.EVENT.register(this::destroy);
        JoinWorldCallback.EVENT.register(this::build);
        GameRenderCallback.EVENT.register(this::render);
        PlayerTickCallback.EVENT.register(this::tick);
    }

    private void tick(ClientPlayerEntity player) {
        items = player.getEntityWorld().getEntitiesByClass(ItemEntity.class, player.getBoundingBox().expand(10000, 500, 10000), i -> true);
    }


    private void render(MatrixStack matrixStack, VertexConsumerProvider.Immediate vertexConsumerProvider, float tickDelta) {
        if (!enabled) return;
        RenderSystem.setShader(GameRenderer::getPositionShader);
        RenderSystem.setShaderColor(r, g, b, 1);
        RenderSystem.disableDepthTest();
        Runnable drawBoxTask =
                solid ? () -> solidBox.setShader(matrixStack.peek().getModel(), RenderSystem.getProjectionMatrix(), RenderSystem.getShader())
                        : () -> wireframeBox.setShader(matrixStack.peek().getModel(), RenderSystem.getProjectionMatrix(), RenderSystem.getShader());
        for (ItemEntity target : items) {
            matrixStack.push();
            matrixStack.translate(
                    target.prevX + (target.getX() - target.prevX) * tickDelta,
                    target.prevY + (target.getY() - target.prevY) * tickDelta,
                    target.prevZ + (target.getZ() - target.prevZ) * tickDelta
            );
            drawBoxTask.run();
            matrixStack.pop();
        }
    }

    private void build() {
        wireframeBox = new VertexBuffer();
        Box bb = new Box(-0.25, -0.0, -0.25, 0.25, 0.5, 0.25);
        RenderUtils.drawOutlinedBox(bb, wireframeBox);

        solidBox = new VertexBuffer();
        RenderUtils.drawSolidBox(bb, solidBox);
    }

    private void destroy() {
        items.clear();
        wireframeBox.close();
        solidBox.close();
    }

    private void registerCommand() {
        LiteralArgumentBuilder<ClientCommandSource> node = literal("itemesp");

        node.then(literal("toggle").executes(c -> {
            enabled = !enabled;
            sendModMessage(new LiteralText("Item ESP " + (enabled ? "enabled" : "disabled")).formatted(Formatting.GOLD));
            return 1;
        }));

        node.then(literal("solid").executes(c -> {
            solid = !solid;
            sendModMessage(new LiteralText("Item ESP " + (solid ? "solid" : "transparent")).formatted(Formatting.GOLD));
            return 1;
        }));

        node.then(literal("color").then(arg("r").then(arg("g").then(arg("b").executes(c -> {
            setColor(c);
            return 0;
        })))));

        node.executes(c -> {
            sendModMessage(new LiteralText("/itemesp toggle"));
            sendModMessage(new LiteralText("/itemesp solid"));
            sendModMessage(new LiteralText("/itemesp size <size>"));
            sendModMessage(new LiteralText("/itemesp color <r> <g> <b>"));
            return 1;
        });
        register(node);
    }

    RequiredArgumentBuilder<ClientCommandSource, Integer> arg(String key) {
        return argument(key, IntegerArgumentType.integer(0, 256));
    }

    private void setColor(CommandContext<ClientCommandSource> c) {
        this.r = c.getArgument("r", Integer.class) / 256f;
        this.g = c.getArgument("g", Integer.class) / 256f;
        this.b = c.getArgument("b", Integer.class) / 256f;
        sendModMessage(new LiteralText("Color updated.").formatted(Formatting.GOLD));
    }
}
