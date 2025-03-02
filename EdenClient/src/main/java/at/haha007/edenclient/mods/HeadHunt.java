package at.haha007.edenclient.mods;

import at.haha007.edenclient.annotations.Mod;
import at.haha007.edenclient.callbacks.*;
import at.haha007.edenclient.utils.PlayerUtils;
import at.haha007.edenclient.utils.RenderUtils;
import at.haha007.edenclient.utils.config.ConfigSubscriber;
import at.haha007.edenclient.utils.config.PerWorldConfig;
import com.mojang.blaze3d.buffers.BufferUsage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientChunkCache;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.CoreShaders;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static at.haha007.edenclient.command.CommandManager.*;
import static at.haha007.edenclient.utils.PlayerUtils.getPlayer;
import static at.haha007.edenclient.utils.PlayerUtils.sendModMessage;

@Mod
public class HeadHunt {
    private boolean enabled;
    @ConfigSubscriber("true")
    boolean tracer;
    @ConfigSubscriber("true")
    boolean clickHeads;
    @ConfigSubscriber("1")
    float red;
    @ConfigSubscriber("1")
    float green;
    @ConfigSubscriber("1")
    float blue;
    Set<Vec3i> heads = new HashSet<>();
    Set<Vec3i> foundHeads = new HashSet<>();
    private VertexBuffer wireframeBox;

    public HeadHunt() {
        GameRenderCallback.EVENT.register(this::render, getClass());
        PlayerTickCallback.EVENT.register(this::tick, getClass());
        JoinWorldCallback.EVENT.register(this::build, getClass());
        LeaveWorldCallback.EVENT.register(this::destroy, getClass());
        PlayerInteractBlockCallback.EVENT.register(this::onInteractBlock, getClass());
        PerWorldConfig.get().register(this, "headhunt");
        registerCommand();
    }

    private InteractionResult onInteractBlock(LocalPlayer localPlayer, ClientLevel clientLevel, InteractionHand interactionHand, BlockHitResult blockHitResult) {
        BlockPos blockPos = blockHitResult.getBlockPos();
        foundHeads.add(blockPos);
        return InteractionResult.PASS;
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
        if(clickHeads) {
            heads.stream()
                    .filter(bp -> player.position().distanceToSqr(Vec3.atCenterOf(bp)) < 20)
                    .forEach(this::clickPos);
        }
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
        wireframeBox = new VertexBuffer(BufferUsage.STATIC_WRITE);
        AABB bb = new AABB(0, 0, 0, 1, 1, 1);
        RenderUtils.drawOutlinedBox(bb, wireframeBox);
    }

    private void destroy() {
        heads = new HashSet<>();
        foundHeads = new HashSet<>();
        wireframeBox.close();
    }

    private void registerCommand() {
        LiteralArgumentBuilder<FabricClientCommandSource> cmd = literal("eheadhunt");
        LiteralArgumentBuilder<FabricClientCommandSource> toggle = literal("toggle");
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

        cmd.then(literal("click").executes(c -> {
            clickHeads = !clickHeads;
            sendModMessage(clickHeads ? "Atomatic click enabled" : "Atomatic click disabled");
            return 1;
        }));

        cmd.then(literal("color").then(arg("r").then(arg("g").then(arg("b").executes(this::setColor)))));

        cmd.then(toggle);
        register(cmd, "HeadHunt is a hack for head searching games.");
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


    private void render(PoseStack matrixStack, MultiBufferSource.BufferSource vertexConsumerProvider, float v) {
        if (!enabled) return;
        if(heads.isEmpty()) return;
        RenderSystem.setShader(Minecraft.getInstance().getShaderManager().getProgram(CoreShaders.POSITION));
        RenderSystem.setShaderColor(red, green, blue, 1);
        if (tracer) {
            matrixStack.pushPose();
            matrixStack.translate(.5, .5, .5);
            Vector3f start = new Vector3f(RenderUtils.getCameraPos().add(PlayerUtils.getClientLookVec()).add(-.5, -.5, -.5).toVector3f());
            Matrix4f matrix = matrixStack.last().pose();
            BufferBuilder bb = Tesselator.getInstance().begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION);

            for (Vec3i t : heads) {
                bb.addVertex(matrix, t.getX(), t.getY(), t.getZ());
                bb.addVertex(matrix, start.x(), start.y(), start.z());
            }
            BufferUploader.drawWithShader(bb.buildOrThrow());
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
