package at.haha007.edenclient.mods;

import at.haha007.edenclient.annotations.Mod;
import at.haha007.edenclient.callbacks.GameRenderCallback;
import at.haha007.edenclient.callbacks.LeaveWorldCallback;
import at.haha007.edenclient.callbacks.PlayerTickCallback;
import at.haha007.edenclient.utils.BakedLineRenderer;
import at.haha007.edenclient.utils.PlayerUtils;
import at.haha007.edenclient.utils.area.BlockArea;
import at.haha007.edenclient.utils.area.BlockAreaRenderFactory;
import at.haha007.edenclient.utils.area.SavableBlockArea;
import at.haha007.edenclient.utils.area.SavableBlockAreaList;
import at.haha007.edenclient.utils.config.ConfigSubscriber;
import at.haha007.edenclient.utils.config.PerWorldConfig;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import fi.dy.masa.malilib.render.RenderUtils;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.chunk.RenderRegionCache;
import net.minecraft.core.Vec3i;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static at.haha007.edenclient.command.CommandManager.*;

@Mod
public class PathTimer {
    private boolean enabled = false;
    private Map<SavableBlockArea, BakedLineRenderer> rendererCache = new HashMap<>();

    @ConfigSubscriber("false")
    private boolean checkpointsEnabled = false;
    @ConfigSubscriber
    private SavableBlockAreaList path;

    public PathTimer() {
        PerWorldConfig.get().register(this, "pathtimer");
        GameRenderCallback.EVENT.register(this::onRender, getClass());
        LeaveWorldCallback.EVENT.register(this::onQuit, getClass());
        registerCommand();
    }

    private void onQuit() {

    }

    private void registerCommand() {
        LiteralArgumentBuilder<FabricClientCommandSource> command = literal("epathtimer");
        command.then(literal("toggle").executes(c -> {
            enabled = !enabled;
            if (enabled) {
                PlayerUtils.sendModMessage("Path timer enabled.");
            } else {
                PlayerUtils.sendModMessage("Path timer disabled.");
            }
            return 1;
        }));
        LiteralArgumentBuilder<FabricClientCommandSource> checkpointCommand = literal("checkpoint").executes(c -> {
            if (checkpointsEnabled) {
                checkpointsEnabled = false;
                PlayerUtils.sendModMessage("Checkpoint areas disabled.");
            } else {
                checkpointsEnabled = true;
                PlayerUtils.sendModMessage("Checkpoint areas enabled.");
            }
            return 1;
        });

        LiteralArgumentBuilder<FabricClientCommandSource> addCommand = literal("add").then(checkpointCommand);
        List<ArgumentBuilder<FabricClientCommandSource, ?>> addCmds = BlockArea.commands((context, area) -> {
            SavableBlockArea savableArea = new SavableBlockArea(area);
            path.add(savableArea);
            PlayerUtils.sendModMessage("Added checkpoint area.");
        });
        for (ArgumentBuilder<FabricClientCommandSource, ?> addCmd : addCmds) {
            addCommand.then(addCmd);
        }
        checkpointCommand.then(addCommand);

        checkpointCommand.then(literal("list").executes(c -> {
            if (path.isEmpty()) {
                PlayerUtils.sendModMessage("No checkpoint areas defined.");
            } else {
                PlayerUtils.sendModMessage("Checkpoint areas:");
                for (int i = 0; i < path.size(); i++) {
                    PlayerUtils.sendModMessage(" " + (i + 1) + ": " + path.get(i).toString());
                }
            }
            return 1;
        }));

        checkpointCommand.then(literal("clear").executes(c -> {
            path.clear();
            PlayerUtils.sendModMessage("Cleared all checkpoint areas.");
            return 1;
        }));

        checkpointCommand.then(literal("remove").then(argument("<index>", IntegerArgumentType.integer()).executes(c -> {
            int index = IntegerArgumentType.getInteger(c, "<index>") - 1;
            if (index < 0 || index >= path.size()) {
                PlayerUtils.sendModMessage("Invalid index. There are " + path.size() + " checkpoint areas.");
                return 1;
            }
            path.remove(index);
            PlayerUtils.sendModMessage("Removed checkpoint area " + (index + 1) + ".");
            return 1;
        })));

        checkpointCommand.then(literal("nearest").executes(c -> {
            LocalPlayer player = PlayerUtils.getPlayer();
            if (path.isEmpty()) {
                PlayerUtils.sendModMessage("No checkpoint areas defined.");
                return 1;
            }
            SavableBlockArea nearestArea = null;
            int nearestIndex = -1;
            double nearestDistance = Double.MAX_VALUE;
            for (int i = 0; i < path.size(); i++) {
                SavableBlockArea area = path.get(i);
                double distance = area.center().getCenter().distanceTo(player.position());
                if (distance < nearestDistance) {
                    nearestDistance = distance;
                    nearestArea = area;
                    nearestIndex = i;
                }
            }
            if (nearestArea != null) {
                PlayerUtils.sendModMessage("Nearest checkpoint area: " + (nearestIndex + 1) + " (Distance: " + String.format("%.2f", nearestDistance) + ")");
            }
            return 1;
        }));

        command.then(checkpointCommand);
        register(command, "Track checkpoint times.");
    }

    private void onRender(float deltaTick) {
        if(path == null) return;
        float xRot = Minecraft.getInstance().getCameraEntity().getXRot();
        float yRot = Minecraft.getInstance().getCameraEntity().getYRot();
        for (int i = 0; i < path.size(); i++) {
            SavableBlockArea area = path.get(i);
            Runnable renderer = rendererCache.computeIfAbsent(area, BlockAreaRenderFactory::createBakedRenderer);
            Vec3 center = area.center().getCenter();
            RenderUtils.drawTextPlate(List.of("" + (i + 1)), center.x(), center.y() + .25, center.z(), yRot, xRot, .05f, 0xFFFFFFFF, 0x40000000, false);
            renderer.run();
        }
    }

    private Component formatTimeComponent(long total, long delta) {
        String totalStr = formatDeltaTime(total);
        String deltaStr = formatDeltaTime(delta);
        return Component.empty()
                .append(Component.text(deltaStr, NamedTextColor.GOLD, TextDecoration.BOLD))
                .append(Component.text(" [" + totalStr + "]", NamedTextColor.YELLOW));
    }

    private String formatDeltaTime(long deltaTime) {
        long seconds = deltaTime / 1000;
        long milliseconds = deltaTime % 1000;
        long minutes = seconds / 60;
        seconds = seconds % 60;
        return String.format("%02d:%02d.%03d", minutes, seconds, milliseconds);
    }

}
