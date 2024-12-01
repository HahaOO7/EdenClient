package at.haha007.edenclient.mods;

import at.haha007.edenclient.annotations.Mod;
import at.haha007.edenclient.callbacks.GameRenderCallback;
import at.haha007.edenclient.callbacks.JoinWorldCallback;
import at.haha007.edenclient.callbacks.LeaveWorldCallback;
import at.haha007.edenclient.callbacks.PlayerTickCallback;
import at.haha007.edenclient.utils.RenderUtils;
import at.haha007.edenclient.utils.config.ConfigSubscriber;
import at.haha007.edenclient.utils.config.PerWorldConfig;
import com.mojang.blaze3d.buffers.BufferUsage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.CoreShaders;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

import static at.haha007.edenclient.command.CommandManager.*;
import static at.haha007.edenclient.utils.PlayerUtils.sendModMessage;

@Mod
public class ItemEsp {
    @ConfigSubscriber("false")
    boolean enabled = false;
    @ConfigSubscriber("1")
    float red;
    @ConfigSubscriber("1")
    float green;
    @ConfigSubscriber("1")
    float blue;
    @ConfigSubscriber("false")
    boolean solid;
    List<ItemEntity> items = new ArrayList<>();
    private VertexBuffer wireframeBox;
    private VertexBuffer solidBox;


    public ItemEsp() {
        registerCommand();
        PerWorldConfig.get().register(this, "itemEsp");
        LeaveWorldCallback.EVENT.register(this::destroy, getClass());
        JoinWorldCallback.EVENT.register(this::build, getClass());
        GameRenderCallback.EVENT.register(this::render, getClass());
        PlayerTickCallback.EVENT.register(this::tick, getClass());
    }

    private void tick(LocalPlayer player) {
        items = player.getCommandSenderWorld().getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(10000, 500, 10000), i -> true);
    }

    private void render(PoseStack matrixStack, MultiBufferSource.BufferSource vertexConsumerProvider, float tickDelta) {
        if (!enabled) return;
        RenderSystem.setShader(Minecraft.getInstance().getShaderManager().getProgram(CoreShaders.POSITION));
        RenderSystem.setShaderColor(red, green, blue, 1);
        RenderSystem.disableDepthTest();
        Runnable drawBoxTask = solid ? () -> draw(solidBox, matrixStack) : () -> draw(wireframeBox, matrixStack);
        for (ItemEntity target : items) {
            matrixStack.pushPose();
            Vec3 position = target.getPosition(tickDelta);
            matrixStack.translate(position.x, position.y, position.z);
            drawBoxTask.run();
            matrixStack.popPose();
        }
    }

    private void draw(VertexBuffer buffer, PoseStack matrixStack) {
        buffer.bind();
        buffer.drawWithShader(matrixStack.last().pose(), RenderSystem.getProjectionMatrix(), Minecraft.getInstance().getShaderManager().getProgram(CoreShaders.POSITION));
        VertexBuffer.unbind();
    }

    private void build() {
        wireframeBox = new VertexBuffer(BufferUsage.STATIC_WRITE);
        AABB bb = new AABB(-0.25, -0.0, -0.25, 0.25, 0.5, 0.25);
        RenderUtils.drawOutlinedBox(bb, wireframeBox);

        solidBox = new VertexBuffer(BufferUsage.STATIC_WRITE);
        RenderUtils.drawSolidBox(bb, solidBox);
    }

    private void destroy() {
        items.clear();
        wireframeBox.close();
        solidBox.close();
    }

    private void registerCommand() {
        LiteralArgumentBuilder<FabricClientCommandSource> node = literal("eitemesp");

        node.then(literal("toggle").executes(c -> {
            enabled = !enabled;
            sendModMessage(("Item ESP " + (enabled ? "enabled" : "disabled")));
            return 1;
        }));

        node.then(literal("solid").executes(c -> {
            solid = !solid;
            sendModMessage(("Item ESP " + (solid ? "solid" : "transparent")));
            return 1;
        }));

        node.then(literal("color").then(arg("r").then(arg("g").then(arg("b").executes(c -> {
            setColor(c);
            return 0;
        })))));

        node.executes(c -> {
            sendModMessage("/itemesp toggle");
            sendModMessage("/itemesp solid");
            sendModMessage("/itemesp size <size>");
            sendModMessage("/itemesp color <r> <g> <b>");
            return 1;
        });

        register(node,
                "ItemESP allows for all items lying on the ground to be surrounded with their respective x-ray bounding boxes.");
    }

    RequiredArgumentBuilder<FabricClientCommandSource, Integer> arg(String key) {
        return argument(key, IntegerArgumentType.integer(0, 256));
    }

    private void setColor(CommandContext<FabricClientCommandSource> c) {
        this.red = c.getArgument("r", Integer.class) / 256f;
        this.green = c.getArgument("g", Integer.class) / 256f;
        this.blue = c.getArgument("b", Integer.class) / 256f;
        sendModMessage("Color updated.");
    }
}
