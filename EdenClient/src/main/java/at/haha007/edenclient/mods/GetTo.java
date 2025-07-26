package at.haha007.edenclient.mods;

import at.haha007.edenclient.EdenClient;
import at.haha007.edenclient.annotations.Mod;
import at.haha007.edenclient.callbacks.GameRenderCallback;
import at.haha007.edenclient.callbacks.PlayerTickCallback;
import at.haha007.edenclient.mods.datafetcher.DataFetcher;
import at.haha007.edenclient.utils.EdenRenderUtils;
import at.haha007.edenclient.utils.PlayerUtils;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.xpple.clientarguments.arguments.CBlockPosArgument;
import fi.dy.masa.malilib.render.RenderUtils;
import fi.dy.masa.malilib.util.data.Color4f;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.opengl.GL11;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static at.haha007.edenclient.command.CommandManager.*;

@Mod
public class GetTo {
    private Vec3i target;
    private boolean tracer;
    private boolean box;
    private static final String COMMAND_NAME = "egetto";

    public GetTo() {
        registerCommand();
        GameRenderCallback.EVENT.register(f -> render(), getClass());
        PlayerTickCallback.EVENT.register(this::tick, getClass());
    }

    private void tick(LocalPlayer player) {
        if (target == null) return;
        if (player.blockPosition().distSqr(target) < 10) target = null;
    }

    private void render() {
        if (target == null) return;
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        if (box) {
            RenderUtils.renderAreaOutline(new BlockPos(target), new BlockPos(target), 10, Color4f.WHITE, Color4f.WHITE, Color4f.WHITE);
        }
        if (tracer) {
            EdenRenderUtils.drawTracers(List.of(Vec3.atCenterOf(target)), Color4f.WHITE);
        }
    }

    private void registerCommand() {
        LiteralArgumentBuilder<FabricClientCommandSource> cmd = literal(COMMAND_NAME);
        cmd.then(argument("target", CBlockPosArgument.blockPos()).executes(c -> {
            BlockPos pos = CBlockPosArgument.getBlockPos(c, "target");
            getTo(pos, true, true, true);
            return 1;
        }).then(argument("tags", StringArgumentType.word()).suggests((c, b) -> {
            b.suggest("-t");
            b.suggest("-b");
            b.suggest("-p");
            b.suggest("-tbp");
            return b.buildFuture();
        }).executes(c -> {
            BlockPos pos = CBlockPosArgument.getBlockPos(c, "target");
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
        return String.format("/%s %d %d %d", COMMAND_NAME, target.getX(), target.getY(), target.getZ());
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
        return EdenClient.getMod(DataFetcher.class).getPlayerWarps().getWarps().stream()
                .min(Comparator.comparingDouble(e -> e.pos().distSqr(pos)))
                .map(e -> dist(pos, pp) < dist(e.pos(), pos) ? null : e.name());
    }

    private double dist(Vec3i a, Vec3i b) {
        return a.distSqr(b);
    }
}