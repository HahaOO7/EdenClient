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
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.VertexBuffer;
import net.minecraft.client.network.ClientCommandSource;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientChunkManager;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.*;
import net.minecraft.world.chunk.WorldChunk;

import java.util.*;
import java.util.stream.Collectors;

import static at.haha007.edenclient.command.CommandManager.*;
import static at.haha007.edenclient.utils.PlayerUtils.getPlayer;
import static at.haha007.edenclient.utils.PlayerUtils.sendModMessage;

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

    private void tick(ClientPlayerEntity player) {
        if (!enabled) {
            heads = new HashSet<>();
            foundHeads = new HashSet<>();
            return;
        }
        ChunkPos chunkPos = player.getChunkPos();
        ClientChunkManager cm = player.clientWorld.getChunkManager();
        BlockPos pp = player.getBlockPos();
        heads = ChunkPos.stream(chunkPos, 20)
                .flatMap(cp -> {
                    WorldChunk wc = cm.getWorldChunk(cp.x, cp.z, false);
                    if (wc == null) return null;
                    return wc.getBlockEntities().entrySet().stream();
                })
                .filter(e -> e.getValue().getType() == BlockEntityType.SKULL)
                .map(Map.Entry::getKey)
                .sorted(Comparator.comparingDouble(pos -> pos.getSquaredDistance(pp)))
                .limit(1000)
                .map(v -> (Vec3i) v).collect(Collectors.toSet());
        heads.removeAll(foundHeads);
        heads.stream().filter(bp -> player.getPos().squaredDistanceTo(Vec3d.ofCenter(bp)) < 20).forEach(this::clickPos);

        System.out.println("Heads found: " + heads.size());
    }

    private void clickPos(Vec3i target) {
        BlockPos bp = new BlockPos(target);
        Direction dir = Direction.UP;
        ClientPlayerInteractionManager im = MinecraftClient.getInstance().interactionManager;
        if (im == null) return;
        im.interactBlock(getPlayer(), Hand.MAIN_HAND, new BlockHitResult(Vec3d.of(bp.offset(dir)), dir, bp, false));
        foundHeads.add(target);
        System.out.println("Head clicked: " + target);
    }

    private void build() {
        foundHeads.clear();
        enabled = false;
        wireframeBox = new VertexBuffer();
        Box bb = new Box(0, 0, 0, 1, 1, 1);
        RenderUtils.drawOutlinedBox(bb, wireframeBox);
    }

    private void destroy() {
        heads = new HashSet<>();
        foundHeads = new HashSet<>();
        wireframeBox.close();
    }

    private void registerCommand() {
        LiteralArgumentBuilder<ClientCommandSource> cmd = literal("eheadhunt");
        LiteralArgumentBuilder<ClientCommandSource> toggle = literal("toggle");
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


    private void render(MatrixStack matrixStack, VertexConsumerProvider.Immediate vertexConsumerProvider, float v) {
        if (!enabled) return;
        RenderSystem.setShader(GameRenderer::getPositionShader);
        RenderSystem.setShaderColor(r, g, b, 1);
        if (tracer) {
            matrixStack.push();
            matrixStack.translate(.5, .5, .5);
            Vec3f start = new Vec3f(RenderUtils.getCameraPos().add(PlayerUtils.getClientLookVec()).add(-.5, -.5, -.5));
            Matrix4f matrix = matrixStack.peek().getPositionMatrix();
            BufferBuilder bb = Tessellator.getInstance().getBuffer();

            bb.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION);
            for (Vec3i t : heads) {
                bb.vertex(matrix, t.getX(), t.getY(), t.getZ()).next();
                bb.vertex(matrix, start.getX(), start.getY(), start.getZ()).next();
            }
            BufferRenderer.drawWithoutShader(Objects.requireNonNull(bb.end()));
            matrixStack.pop();
        }

        heads.forEach(c -> {
            matrixStack.push();
            matrixStack.translate(c.getX(), c.getY(), c.getZ());
            wireframeBox.bind();
            wireframeBox.draw(matrixStack.peek().getPositionMatrix(), RenderSystem.getProjectionMatrix(), RenderSystem.getShader());
            VertexBuffer.unbind();
            matrixStack.pop();
        });
    }
}
