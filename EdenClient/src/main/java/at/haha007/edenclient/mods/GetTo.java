package at.haha007.edenclient.mods;

import at.haha007.edenclient.EdenClient;
import at.haha007.edenclient.annotations.Mod;
import at.haha007.edenclient.callbacks.GameRenderCallback;
import at.haha007.edenclient.callbacks.JoinWorldCallback;
import at.haha007.edenclient.callbacks.LeaveWorldCallback;
import at.haha007.edenclient.callbacks.PlayerTickCallback;
import at.haha007.edenclient.mods.datafetcher.DataFetcher;
import at.haha007.edenclient.utils.PlayerUtils;
import at.haha007.edenclient.utils.RenderUtils;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.logging.LogUtils;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.commands.arguments.coordinates.Coordinates;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.Comparator;
import java.util.Optional;

import static at.haha007.edenclient.command.CommandManager.*;

@Mod
public class GetTo {
    private Vec3i target;
    private VertexBuffer vb;
    private boolean tracer;
    private boolean box;
    private final String commandName = "egetto";

    public GetTo() {
        registerCommand();
        JoinWorldCallback.EVENT.register(this::build, getClass());
        LeaveWorldCallback.EVENT.register(this::destroy, getClass());
        GameRenderCallback.EVENT.register(this::render, getClass());
        PlayerTickCallback.EVENT.register(this::tick, getClass());
    }

    private void destroy() {
        vb.close();
    }

    private void build() {
        vb = new VertexBuffer(VertexBuffer.Usage.STATIC);
        AABB bb = new AABB(0, 0, 0, 1, 1, 1);
        RenderUtils.drawOutlinedBox(bb, vb);
    }

    private void tick(LocalPlayer player) {
        if (target == null) return;
        if (player.blockPosition().distSqr(target) < 10) target = null;
    }

    private void render(PoseStack matrixStack, MultiBufferSource.BufferSource vertexConsumerProvider, float deltaTick) {
        if (target == null) return;
        RenderSystem.setShader(GameRenderer::getPositionShader);
        RenderSystem.setShaderColor(1, 1, 1, 1);
        RenderSystem.disableDepthTest();

        if (box) {
            matrixStack.pushPose();
            matrixStack.translate(target.getX(), target.getY(), target.getZ());
            this.vb.drawWithShader(matrixStack.last().pose(), RenderSystem.getProjectionMatrix(), RenderSystem.getShader());
            matrixStack.popPose();
        }
        if (tracer) {
            Matrix4f matrix = matrixStack.last().pose();
            Vec3 start = RenderUtils.getCameraPos().add(PlayerUtils.getClientLookVec());
            BufferBuilder bb = Tesselator.getInstance().begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION);
            bb.addVertex(matrix, target.getX() + .5f, target.getY() + .5f, target.getZ() + .5f);
            bb.addVertex(matrix, (float) start.x, (float) start.y, (float) start.z);
            RenderSystem.setShaderColor(1, 1, 1, 1);
            BufferUploader.drawWithShader(bb.buildOrThrow());
        }
    }

    private void registerCommand() {
        LiteralArgumentBuilder<ClientSuggestionProvider> cmd = literal(commandName);
        cmd.then(argument("target", BlockPosArgument.blockPos()).executes(c -> {
            BlockPos pos = c.getArgument("target", Coordinates.class).getBlockPos(PlayerUtils.getPlayer().createCommandSourceStack());
            getTo(pos, true, true, true);
            return 1;
        }).then(argument("tags", StringArgumentType.word()).suggests((c, b) -> {
            b.suggest("-t");
            b.suggest("-b");
            b.suggest("-p");
            b.suggest("-tbp");
            return b.buildFuture();
        }).executes(c -> {
            BlockPos pos = c.getArgument("target", Coordinates.class).getBlockPos(PlayerUtils.getPlayer().createCommandSourceStack());
            String tags = c.getArgument("tags", String.class);
            if (!tags.startsWith("-")) {
                PlayerUtils.sendModMessage("/getto <target> -[t,b,p]");
                PlayerUtils.sendModMessage("-t -> Tracer");
                PlayerUtils.sendModMessage("-b -> Box");
                PlayerUtils.sendModMessage("-p -> Teleport");
                return -1;
            }
            tags = tags.substring(1);
            getTo(pos, tags.contains("t"), tags.contains("b"), tags.contains("p"));
            return 1;
        })));
        cmd.then(literal("clear").executes(c -> {
            target = null;
            PlayerUtils.sendModMessage("Target disabled");
            return 1;
        }));
        register(cmd, "This mod is for internal use only.");
    }

    public String getCommandTo(Vec3i target) {
        return String.format("/%s %d %d %d", commandName, target.getX(), target.getY(), target.getZ());
    }

    private void getTo(BlockPos pos, boolean tracer, boolean box, boolean tp) {
        target = pos;
        this.tracer = tracer;
        this.box = box;
        if (tp) {
            getNearestPlayerWarp(pos).map(pw -> "/pwarp " + pw).ifPresent(PlayerUtils::messageC2S);
        }
    }

    private Optional<String> getNearestPlayerWarp(Vec3i pos) {
        Vec3i pp = PlayerUtils.getPlayer().blockPosition();
        LogUtils.getLogger().info(EdenClient.getMod(DataFetcher.class).getPlayerWarps().getWarps().toString());
        return EdenClient.getMod(DataFetcher.class).getPlayerWarps().getWarps().stream()
                .min(Comparator.comparingDouble(e -> e.pos().distSqr(pos)))
                .map(e -> dist(pos, pp) < dist(e.pos(), pos) ? null : e.name());
    }

    private double dist(Vec3i a, Vec3i b) {
        return a.distSqr(b);
    }
}