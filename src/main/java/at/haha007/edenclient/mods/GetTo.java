package at.haha007.edenclient.mods;

import at.haha007.edenclient.EdenClient;
import at.haha007.edenclient.callbacks.ConfigLoadCallback;
import at.haha007.edenclient.callbacks.ConfigSaveCallback;
import at.haha007.edenclient.callbacks.GameRenderCallback;
import at.haha007.edenclient.callbacks.PlayerTickCallback;
import at.haha007.edenclient.utils.PlayerUtils;
import at.haha007.edenclient.utils.RenderUtils;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.client.gl.VertexBuffer;
import net.minecraft.client.network.ClientCommandSource;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.command.argument.PosArgument;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.*;

import java.util.Comparator;
import java.util.Optional;

import static at.haha007.edenclient.command.CommandManager.*;

public class GetTo {
    private Vec3i target;
    private VertexBuffer vb;
    private boolean tracer, box;

    public GetTo() {
        registerCommand();
        ConfigLoadCallback.EVENT.register(this::load);
        ConfigSaveCallback.EVENT.register(this::save);
        GameRenderCallback.EVENT.register(this::render);
        PlayerTickCallback.EVENT.register(this::tick);
    }

    private void save(NbtCompound nbtCompound) {
        vb.close();
    }

    private void load(NbtCompound nbtCompound) {
        vb = new VertexBuffer();
        Box bb = new Box(0, 0, 0, 1, 1, 1);
        RenderUtils.drawOutlinedBox(bb, vb);
    }

    private void tick(ClientPlayerEntity player) {
        if (target == null) return;
        if (player.getBlockPos().getSquaredDistance(target) < 10) target = null;
    }

    private void render(MatrixStack matrixStack, VertexConsumerProvider.Immediate vertexConsumerProvider, float deltaTick) {
        if (target == null) return;
        RenderSystem.setShader(GameRenderer::getPositionShader);
        RenderSystem.setShaderColor(1, 1, 1, 1);
        RenderSystem.disableDepthTest();

        if (box) {
            matrixStack.push();
            matrixStack.translate(target.getX(), target.getY(), target.getZ());
            this.vb.setShader(matrixStack.peek().getModel(), RenderSystem.getProjectionMatrix(), RenderSystem.getShader());
            matrixStack.pop();
        }
        if (tracer) {
            Matrix4f matrix = matrixStack.peek().getModel();
            Vec3d start = RenderUtils.getCameraPos().add(PlayerUtils.getClientLookVec());
            BufferBuilder bb = Tessellator.getInstance().getBuffer();
            bb.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION);
            bb.vertex(matrix, target.getX() + .5f, target.getY() + .5f, target.getZ() + .5f).next();
            bb.vertex(matrix, (float) start.x, (float) start.y, (float) start.z).next();
            bb.end();
            RenderSystem.setShaderColor(1, 1, 1, 1);
            BufferRenderer.draw(bb);
        }
    }

    private void registerCommand() {
        LiteralArgumentBuilder<ClientCommandSource> cmd = literal("getto");
        cmd.then(argument("target", BlockPosArgumentType.blockPos()).executes(c -> {
            BlockPos pos = c.getArgument("target", PosArgument.class).toAbsoluteBlockPos(PlayerUtils.getPlayer().getCommandSource());
            getTo(pos, true, true, true);
            return 1;
        }).then(argument("tags", StringArgumentType.word()).suggests((c, b) -> {
            System.out.println(b.getInput());
            b.suggest("-t");
            b.suggest("-b");
            b.suggest("-p");
            b.suggest("-tbp");
            return b.buildFuture();
        }).executes(c -> {
            BlockPos pos = c.getArgument("target", PosArgument.class).toAbsoluteBlockPos(PlayerUtils.getPlayer().getCommandSource());
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
        register(cmd);
    }

    private void getTo(BlockPos pos, boolean tracer, boolean box, boolean tp) {
        target = pos;
        this.tracer = tracer;
        this.box = box;
        if (tp)
            getNearestPlayerWarp(pos).map(pw -> "/pw " + pw).ifPresent(PlayerUtils::messageC2S);
    }

    private Optional<String> getNearestPlayerWarp(Vec3i pos) {
        Vec3i pp = PlayerUtils.getPlayer().getBlockPos();
        return EdenClient.INSTANCE.getDataFetcher().getPlayerWarps().getAll().entrySet().stream()
                .min(Comparator.comparingDouble(e -> e.getValue().getSquaredDistance(pos)))
                .map(e -> dist(pos, pp) < dist(e.getValue(), pos) ? null : e.getKey());
    }

    private double dist(Vec3i a, Vec3i b) {
        return a.getSquaredDistance(b);
    }
}
