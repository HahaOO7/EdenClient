package at.haha007.edenclient.mods;

import at.haha007.edenclient.callbacks.GameRenderCallback;
import at.haha007.edenclient.callbacks.JoinWorldCallback;
import at.haha007.edenclient.callbacks.LeaveWorldCallback;
import at.haha007.edenclient.callbacks.PlayerTickCallback;
import at.haha007.edenclient.utils.ChatColor;
import at.haha007.edenclient.utils.RenderUtils;
import at.haha007.edenclient.utils.config.ConfigSubscriber;
import at.haha007.edenclient.utils.config.PerWorldConfig;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;

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
        PerWorldConfig.get().register(this, "itemEsp");
        LeaveWorldCallback.EVENT.register(this::destroy);
        JoinWorldCallback.EVENT.register(this::build);
        GameRenderCallback.EVENT.register(this::render);
        PlayerTickCallback.EVENT.register(this::tick);
    }

    private void tick(LocalPlayer player) {
        items = player.getCommandSenderWorld().getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(10000, 500, 10000), i -> true);
    }

    private void render(PoseStack matrixStack, MultiBufferSource.BufferSource vertexConsumerProvider, float tickDelta) {
        if (!enabled) return;
        RenderSystem.setShader(GameRenderer::getPositionShader);
        RenderSystem.setShaderColor(r, g, b, 1);
        RenderSystem.disableDepthTest();
        Runnable drawBoxTask = solid ? () -> draw(solidBox, matrixStack) : () -> draw(wireframeBox, matrixStack);
        for (ItemEntity target : items) {
            matrixStack.pushPose();
            matrixStack.translate(
                    target.xo + (target.getX() - target.xo) * tickDelta,
                    target.yo + (target.getY() - target.yo) * tickDelta,
                    target.zo + (target.getZ() - target.zo) * tickDelta
            );
            drawBoxTask.run();
            matrixStack.popPose();
        }
    }

    private void draw(VertexBuffer solidBox, PoseStack matrixStack) {
        solidBox.bind();
        solidBox.drawWithShader(matrixStack.last().pose(), RenderSystem.getProjectionMatrix(), RenderSystem.getShader());
        VertexBuffer.unbind();
    }

    private void build() {
        wireframeBox = new VertexBuffer(VertexBuffer.Usage.STATIC);
        AABB bb = new AABB(-0.25, -0.0, -0.25, 0.25, 0.5, 0.25);
        RenderUtils.drawOutlinedBox(bb, wireframeBox);

        solidBox = new VertexBuffer(VertexBuffer.Usage.STATIC);
        RenderUtils.drawSolidBox(bb, solidBox);
    }

    private void destroy() {
        items.clear();
        wireframeBox.close();
        solidBox.close();
    }

    private void registerCommand() {
        LiteralArgumentBuilder<ClientSuggestionProvider> node = literal("eitemesp");

        node.then(literal("toggle").executes(c -> {
            enabled = !enabled;
            sendModMessage(ChatColor.GOLD + ("Item ESP " + (enabled ? "enabled" : "disabled")));
            return 1;
        }));

        node.then(literal("solid").executes(c -> {
            solid = !solid;
            sendModMessage(ChatColor.GOLD + ("Item ESP " + (solid ? "solid" : "transparent")));
            return 1;
        }));

        node.then(literal("color").then(arg("r").then(arg("g").then(arg("b").executes(c -> {
            setColor(c);
            return 0;
        })))));

        node.executes(c -> {
            sendModMessage(ChatColor.GOLD + "/itemesp toggle");
            sendModMessage(ChatColor.GOLD + "/itemesp solid");
            sendModMessage(ChatColor.GOLD + "/itemesp size <size>");
            sendModMessage(ChatColor.GOLD + "/itemesp color <r> <g> <b>");
            return 1;
        });

        register(node,
                "ItemESP allows for all items lying on the ground to be surrounded with their respective x-ray bounding boxes.");
    }

    RequiredArgumentBuilder<ClientSuggestionProvider, Integer> arg(String key) {
        return argument(key, IntegerArgumentType.integer(0, 256));
    }

    private void setColor(CommandContext<ClientSuggestionProvider> c) {
        this.r = c.getArgument("r", Integer.class) / 256f;
        this.g = c.getArgument("g", Integer.class) / 256f;
        this.b = c.getArgument("b", Integer.class) / 256f;
        sendModMessage(ChatColor.GOLD + "Color updated.");
    }
}
