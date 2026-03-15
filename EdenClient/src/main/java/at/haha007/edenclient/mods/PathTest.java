package at.haha007.edenclient.mods;

import at.haha007.edenclient.annotations.Mod;
import at.haha007.edenclient.callbacks.GameRenderCallback;
import at.haha007.edenclient.callbacks.PlayerTickCallback;
import at.haha007.edenclient.utils.EdenRenderUtils;
import at.haha007.edenclient.utils.PlayerUtils;
import at.haha007.edenclient.utils.config.PerWorldConfig;
import at.haha007.edenclient.utils.pathing.*;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import fi.dy.masa.malilib.render.RenderUtils;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import static at.haha007.edenclient.command.CommandManager.literal;
import static at.haha007.edenclient.command.CommandManager.register;

@Mod
public class PathTest {
    private Path path;

    public PathTest() {
        generatePath();
        registerCommand();
        GameRenderCallback.EVENT.register(this::render, getClass());
        PlayerTickCallback.EVENT.register(this::tick, getClass());
    }

    private void tick(LocalPlayer localPlayer) {
        PathPosition nearest = path.getNearest(localPlayer.position());
        int segment = nearest.block();
        if ((segment > 0 || path.getBlock(segment).endPos().distanceToSqr(localPlayer.position()) < .4)
                && nearest.distance() < .5
                && path.length() > 1) {
            path = path.subPath(1);
        }

        PathFollower.follow(path);
    }

    private void generatePath() {
        List<Vec3i> points = new ArrayList<>();
        Random random = ThreadLocalRandom.current();
        for (int i = 0; i < 5; i++) {
            points.add(PlayerUtils.getPlayer().blockPosition().offset(
                    random.nextInt(-10, 10),
                    0,
                    random.nextInt(-10, 10)));
        }

        path = new Path(points);
    }

    private void render(float tickDelta) {
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        PathRenderer.renderPath(path, tickDelta);
    }


    private void registerCommand() {
        LiteralArgumentBuilder<FabricClientCommandSource> node = literal("epathtest");

        node.executes(c -> {
            generatePathTowards();
            return 1;
        });

        register(node);
    }

    private void generatePathTowards() {
        BlockPos playerPos = PlayerUtils.getPlayer().blockPosition();
        Vec3 direction = Minecraft.getInstance().getCameraEntity().getLookAngle();
        direction = direction.multiply(1, 0, 1).normalize().scale(10);
        BlockPos target = playerPos.offset((int) direction.x(), (int) direction.y(), (int) direction.z());
        Path tempPath = SimplePathFinder.findPath(playerPos, target);
        path = tempPath == null ? path : tempPath;
    }

}
