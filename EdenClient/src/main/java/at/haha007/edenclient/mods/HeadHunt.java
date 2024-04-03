package at.haha007.edenclient.mods;

import at.haha007.edenclient.annotations.Mod;
import at.haha007.edenclient.callbacks.GameRenderCallback;
import at.haha007.edenclient.callbacks.JoinWorldCallback;
import at.haha007.edenclient.callbacks.LeaveWorldCallback;
import at.haha007.edenclient.callbacks.PlayerTickCallback;
import at.haha007.edenclient.utils.ChatColor;
import at.haha007.edenclient.utils.PlayerUtils;
import at.haha007.edenclient.utils.RenderUtils;
import at.haha007.edenclient.utils.config.ConfigSubscriber;
import at.haha007.edenclient.utils.config.PerWorldConfig;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientChunkCache;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.*;
import java.util.stream.Collectors;

import static at.haha007.edenclient.command.CommandManager.*;
import static at.haha007.edenclient.utils.PlayerUtils.getPlayer;
import static at.haha007.edenclient.utils.PlayerUtils.sendModMessage;

@Mod
public class HeadHunt {
    private boolean enabled;
    @ConfigSubscriber("true")
    boolean tracer;
    @ConfigSubscriber("1")
    float r, g, b;
    Set<Vec3i> heads = new HashSet<>();
    Set<Vec3i> foundHeads = new HashSet<>();
    private VertexBuffer wireframeBox;

    public HeadHunt() {
        GameRenderCallback.EVENT.register(this::render);
        PlayerTickCallback.EVENT.register(this::tick);
        JoinWorldCallback.EVENT.register(this::build);
        LeaveWorldCallback.EVENT.register(this::destroy);
        PerWorldConfig.get().register(this, "headhunt");
        registerCommand();
    }

    private void tick(LocalPlayer player) {
        if (!enabled) {
            heads = new HashSet<>();
            foundHeads = new HashSet<>();
            return;
        }
        ChunkPos chunkPos = player.chunkPosition();
        ClientChunkCache cm = player.clientLevel.getChunkSource();
        BlockPos pp = player.blockPosition();
        heads = ChunkPos.rangeClosed(chunkPos, 20)
                .flatMap(cp -> {
                    LevelChunk wc = cm.getChunk(cp.x, cp.z, false);
                    if (wc == null) return null;
                    return wc.getBlockEntities().entrySet().stream();
                })
                .filter(e -> e.getValue().getType() == BlockEntityType.SKULL)
                .map(Map.Entry::getKey)
                .sorted(Comparator.comparingDouble(pos -> pos.distSqr(pp)))
                .limit(1000)
                .map(v -> (Vec3i) v).collect(Collectors.toSet());
        heads.removeAll(foundHeads);
        heads.stream().filter(bp -> player.position().distanceToSqr(Vec3.atCenterOf(bp)) < 20).forEach(this::clickPos);
    }

    private void clickPos(Vec3i target) {
        BlockPos bp = new BlockPos(target);
        Direction dir = Direction.UP;
        MultiPlayerGameMode im = Minecraft.getInstance().gameMode;
        if (im == null) return;
        im.useItemOn(getPlayer(), InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atLowerCornerOf(bp.relative(dir)), dir, bp, false));
        foundHeads.add(target);
    }

    private void build() {
        foundHeads.clear();
        enabled = false;
        wireframeBox = new VertexBuffer(VertexBuffer.Usage.STATIC);
        AABB bb = new AABB(0, 0, 0, 1, 1, 1);
        RenderUtils.drawOutlinedBox(bb, wireframeBox);
    }

    private void destroy() {
        heads = new HashSet<>();
        foundHeads = new HashSet<>();
        wireframeBox.close();
    }

    private void registerCommand() {
        LiteralArgumentBuilder<ClientSuggestionProvider> cmd = literal("eheadhunt");
        LiteralArgumentBuilder<ClientSuggestionProvider> toggle = literal("toggle");
        toggle.executes(c -> {
            enabled = !enabled;
            sendModMessage(enabled ? "HeadHunt enabled" : "HeadHunt disabled");
            return 1;
        });

        cmd.then(literal("tracer").executes(c -> {
            tracer = !tracer;
            sendModMessage(tracer ? "Tracer enabled" : "Tracer disabled");
            return 1;
        }));

        cmd.then(literal("tracer").executes(c -> {
            tracer = !tracer;
            sendModMessage(tracer ? "Tracer enabled" : "Tracer disabled");
            return 1;
        }));

        cmd.then(literal("color").then(arg("r").then(arg("g").then(arg("b").executes(this::setColor)))));

        cmd.then(toggle);
        register(cmd, "HeadHunt is a hack for head searching games.");
    }

    RequiredArgumentBuilder<ClientSuggestionProvider, Integer> arg(String key) {
        return argument(key, IntegerArgumentType.integer(0, 255));
    }

    private int setColor(CommandContext<ClientSuggestionProvider> c) {
        this.r = c.getArgument("r", Integer.class) / 256f;
        this.g = c.getArgument("g", Integer.class) / 256f;
        this.b = c.getArgument("b", Integer.class) / 256f;
        sendModMessage(ChatColor.GOLD + "Color updated.");
        return 1;
    }


    private void render(PoseStack matrixStack, MultiBufferSource.BufferSource vertexConsumerProvider, float v) {
        if (!enabled) return;
        RenderSystem.setShader(GameRenderer::getPositionShader);
        RenderSystem.setShaderColor(r, g, b, 1);
        if (tracer) {
            matrixStack.pushPose();
            matrixStack.translate(.5, .5, .5);
            Vector3f start = new Vector3f(RenderUtils.getCameraPos().add(PlayerUtils.getClientLookVec()).add(-.5, -.5, -.5).toVector3f());
            Matrix4f matrix = matrixStack.last().pose();
            BufferBuilder bb = Tesselator.getInstance().getBuilder();

            bb.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION);
            for (Vec3i t : heads) {
                bb.vertex(matrix, t.getX(), t.getY(), t.getZ()).endVertex();
                bb.vertex(matrix, start.x(), start.y(), start.z()).endVertex();
            }
            BufferUploader.drawWithShader(Objects.requireNonNull(bb.end()));
            matrixStack.popPose();
        }

        heads.forEach(c -> {
            matrixStack.pushPose();
            matrixStack.translate(c.getX(), c.getY(), c.getZ());
            wireframeBox.bind();
            wireframeBox.drawWithShader(matrixStack.last().pose(), RenderSystem.getProjectionMatrix(), RenderSystem.getShader());
            VertexBuffer.unbind();
            matrixStack.popPose();
        });
    }
}
