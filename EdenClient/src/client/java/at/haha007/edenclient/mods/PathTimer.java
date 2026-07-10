package at.haha007.edenclient.mods;

import at.haha007.edenclient.annotations.Mod;
import at.haha007.edenclient.callbacks.GameRenderCallback;
import at.haha007.edenclient.callbacks.PlayerTickCallback;
import at.haha007.edenclient.utils.PlayerUtils;
import at.haha007.edenclient.utils.area.*;
import at.haha007.edenclient.utils.config.ConfigSubscriber;
import at.haha007.edenclient.utils.config.PerWorldConfig;
import at.haha007.edenclient.utils.config.loaders.ConfigLoader;
import at.haha007.edenclient.utils.config.loaders.LongListLoader;
import at.haha007.edenclient.utils.config.wrappers.LongList;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.logging.LogUtils;
import fi.dy.masa.malilib.render.RenderUtils;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static at.haha007.edenclient.command.CommandManager.*;

@Mod
public class PathTimer {
    private final Map<SavableBlockArea, Runnable> rendererCache = new HashMap<>();
    private boolean enabled = false;
    private int nextCheckpointIndex = 0;
    private long startTime = 0;
    private final List<Long> checkpointTimes = new ArrayList<>();

    @ConfigSubscriber
    private boolean shouldRender = false;
    @ConfigSubscriber("false")
    private boolean checkpointsEnabled = false;
    @ConfigSubscriber
    private String loadedPathKey;
    @ConfigSubscriber
    private PathMap savedPaths = new PathMap();

    public PathTimer() {
        PerWorldConfig.get().register(this, "pathtimer");
        GameRenderCallback.EVENT.register(this::onRender, getClass());
        PlayerTickCallback.EVENT.register(this::onTick, getClass());
        registerCommand();
        PerWorldConfig.get().register(new PathLoader(), Path.class);
        PerWorldConfig.get().register(new PathMapLoader(), PathMap.class);
        PerWorldConfig.get().register(new PathTimeLoader(), PathTimes.class);

    }

    private static class PathLoader implements ConfigLoader<CompoundTag, Path> {
        private static final SavableBlockAreaListLoader AREA_LIST_LOADER = new SavableBlockAreaListLoader();

        @Override
        public @NonNull CompoundTag save(PathTimer.Path path) {
            SavableBlockAreaList areaList = path.path;
            PathTimesList times = path.times;
            CompoundTag tag = new CompoundTag();
            tag.put("areas", AREA_LIST_LOADER.save(areaList));
            tag.put("times", new PathTimesListLoader().save(times));
            return tag;
        }

        @Override
        public @NonNull Path load(@NonNull CompoundTag nbtElement) {
            SavableBlockAreaList areaList = AREA_LIST_LOADER.load(nbtElement.getListOrEmpty("areas"));
            PathTimesList times = new PathTimesListLoader().load(nbtElement.getListOrEmpty("times"));
            return new Path(areaList, times);
        }

        @Override
        public @NonNull CompoundTag parse(@NotNull String s) {
            return new CompoundTag();
        }
    }

    private static class PathMapLoader implements ConfigLoader<CompoundTag, PathMap> {
        private final PathLoader pathLoader = new PathLoader();

        @Override
        public @NonNull CompoundTag save(PathMap pathMap) {
            CompoundTag tag = new CompoundTag();
            for (Map.Entry<String, Path> entry : pathMap.entrySet()) {
                tag.put(entry.getKey(), pathLoader.save(entry.getValue()));
            }
            return tag;
        }

        @Override
        public @NonNull PathMap load(@NonNull CompoundTag nbtElement) {
            PathMap pathMap = new PathMap();
            for (String key : nbtElement.keySet()) {
                CompoundTag pathTag = nbtElement.getCompoundOrEmpty(key);
                Path path = pathLoader.load(pathTag);
                pathMap.put(key, path);
            }
            return pathMap;
        }

        @Override
        public @NonNull CompoundTag parse(@NotNull String s) {
            return new CompoundTag();
        }
    }

    private static class PathTimeLoader implements ConfigLoader<CompoundTag, PathTimes> {
        private final LongListLoader timeListLoader = new LongListLoader();

        @Override
        public @NonNull CompoundTag save(PathTimes pathTimes) {
            CompoundTag tag = new CompoundTag();
            tag.put("times", timeListLoader.save(pathTimes.times));
            tag.putLong("runStartedAt", pathTimes.runStartedAt);
            return tag;
        }

        @Override
        public @NonNull PathTimes load(@NonNull CompoundTag nbtElement) {
            LongList times = new LongList();
            nbtElement.getLongArray("times").ifPresent(e -> {
                for (long l : e) {
                    times.add(l);
                }
            });
            long runStartedAt = nbtElement.getLongOr("runStartedAt", 0L);
            return new PathTimes(times, runStartedAt);
        }

        @Override
        public @NonNull CompoundTag parse(@NotNull String s) {
            return new CompoundTag();
        }
    }

    private static class PathTimesListLoader implements ConfigLoader<ListTag, PathTimesList> {
        private final PathTimeLoader pathTimeLoader = new PathTimeLoader();

        @Override
        public @NonNull ListTag save(PathTimer.PathTimesList value) {
            ListTag listTag = new ListTag();
            for (PathTimes pathTimes : value) {
                listTag.add(pathTimeLoader.save(pathTimes));
            }
            return listTag;
        }

        @Override
        public @NonNull PathTimesList load(@NonNull ListTag nbtElement) {
            PathTimesList pathTimesList = new PathTimesList();
            for (int i = 0; i < nbtElement.size(); i++) {
                CompoundTag pathTimesTag = nbtElement.getCompoundOrEmpty(i);
                PathTimes pathTimes = pathTimeLoader.load(pathTimesTag);
                pathTimesList.add(pathTimes);
            }
            return pathTimesList;
        }

        @Override
        public @NonNull ListTag parse(@NotNull String s) {
            return new ListTag();
        }
    }

    private record PathTimes(LongList times, long runStartedAt) {
    }

    private static class PathTimesList extends ArrayList<PathTimes> {
    }

    private static class PathMap extends HashMap<String, Path> {
    }

    private record Path(SavableBlockAreaList path, PathTimesList times) {
    }

    private void onEnable() {
        enabled = true;
        nextCheckpointIndex = 0;
        checkpointTimes.clear();
        PlayerUtils.sendModMessage("Path timer enabled.");
    }

    private void registerCommand() {
        LiteralArgumentBuilder<FabricClientCommandSource> command = literal("epathtimer");
        command.then(literal("toggle").executes(c -> {
            enabled = !enabled;
            if (enabled) {
                onEnable();
            } else {
                PlayerUtils.sendModMessage("Path timer disabled.");
            }
            return 1;
        }));
        command.then(literal("start").executes(c -> {
            onEnable();
            return 1;
        }));
        command.then(literal("stop").executes(c -> {
            enabled = false;
            PlayerUtils.sendModMessage("Path timer stopped.");
            return 1;
        }));

        command.then(literal("render").executes(c -> {
            shouldRender = !shouldRender;
            if (shouldRender) {
                PlayerUtils.sendModMessage("Path timer rendering enabled.");
            } else {
                PlayerUtils.sendModMessage("Path timer rendering disabled.");
            }
            return 1;
        }));
        command.then(createCheckpointCommand());

        command.then(literal("create-path").then(argument("path", StringArgumentType.word())
                .executes(c -> {
                    String pathKey = StringArgumentType.getString(c, "path");
                    if (savedPaths.containsKey(pathKey)) {
                        PlayerUtils.sendModMessage("Path with key '" + pathKey + "' already exists.");
                        return 1;
                    }
                    savedPaths.put(pathKey, new Path(new SavableBlockAreaList(), new PathTimesList()));
                    PlayerUtils.sendModMessage("Created new path with key '" + pathKey + "'.");
                    loadedPathKey = pathKey;
                    return 1;
                })));
        command.then(literal("load-path").then(argument("path", StringArgumentType.word())
                .suggests((c, b) -> {
                    savedPaths.keySet().forEach(b::suggest);
                    return b.buildFuture();
                })
                .executes(c -> {
                    String pathKey = StringArgumentType.getString(c, "path");
                    if (!savedPaths.containsKey(pathKey)) {
                        PlayerUtils.sendModMessage("No path with key '" + pathKey + "' found.");
                        return 1;
                    }
                    loadedPathKey = pathKey;
                    enabled = false;
                    PlayerUtils.sendModMessage("Loaded path with key '" + pathKey + "'.");
                    return 1;
                })));

        command.then(literal("export-times").then(argument("path", StringArgumentType.word())
                .suggests((c, b) -> {
                    savedPaths.keySet().forEach(b::suggest);
                    return b.buildFuture();
                })
                .executes(c -> {
                    String pathKey = StringArgumentType.getString(c, "path");
                    Path path = savedPaths.get(pathKey);
                    if (path == null) {
                        PlayerUtils.sendModMessage("No path with key '" + pathKey + "' found.");
                        return 1;
                    }
                    File userHome = new File(System.getProperty("user.home"));
                    File downloadsDirectory = new File(userHome, "Downloads");
                    Calendar calendar = Calendar.getInstance();
                    String dateTimeString = "%s_%s_%s_%s_%s_%s".formatted(
                            calendar.get(Calendar.YEAR),
                            calendar.get(Calendar.MONTH),
                            calendar.get(Calendar.DAY_OF_MONTH),
                            calendar.get(Calendar.HOUR_OF_DAY),
                            calendar.get(Calendar.MINUTE),
                            calendar.get(Calendar.SECOND)
                    );
                    File outFile = new File(downloadsDirectory, "%s_%s.csv".formatted(pathKey, dateTimeString));
                    saveTimesToCsv(outFile, path.times);
                    PlayerUtils.sendModMessage(Component.text("Saved as ").append(Component.text(outFile.getName())
                            .clickEvent(ClickEvent.copyToClipboard(outFile.getPath()))));
                    return 1;
                })));
        command.then(literal("reset-times").then(argument("path", StringArgumentType.word())
                .suggests((c, b) -> {
                    savedPaths.keySet().forEach(b::suggest);
                    return b.buildFuture();
                })
                .executes(c -> {
                    String pathKey = StringArgumentType.getString(c, "path");
                    Path path = savedPaths.get(pathKey);
                    if (path == null) {
                        PlayerUtils.sendModMessage("No path with key '" + pathKey + "' found.");
                        return 1;
                    }
                    path.times.clear();
                    PlayerUtils.sendModMessage("Reset times for path '" + pathKey + "'.");
                    return 1;
                })));
        register(command, "Track checkpoint times.");
    }

    private void saveTimesToCsv(File file, PathTimesList pathTimes) {
        try {
            if (!file.exists() && !file.createNewFile()) {
                LogUtils.getLogger().warn("Couldn't save file {}", file);
                return;
            }
        } catch (IOException e) {
            LogUtils.getLogger().warn("Couldn't save file {}", file, e);
            return;
        }
        try (FileOutputStream fos = new FileOutputStream(file);
             BufferedOutputStream bos = new BufferedOutputStream(fos)) {
            for (PathTimes pathTime : pathTimes) {
                String line = "%s, %s%n".formatted(pathTime.runStartedAt, pathTime.times.stream()
                        .map(Object::toString).collect(Collectors.joining(", ")));
                bos.write(line.getBytes());
            }
        } catch (IOException e) {
            LogUtils.getLogger().warn("Couldn't save file: {}", file.getName(), e);
        }
    }

    private @NonNull LiteralArgumentBuilder<FabricClientCommandSource> createCheckpointCommand() {
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

        LiteralArgumentBuilder<FabricClientCommandSource> addCommand = literal("add");
        List<ArgumentBuilder<FabricClientCommandSource, ?>> addCmds = BlockArea.commands((context, area) -> {
            Path path = loadedPath();
            if (path == null) {
                PlayerUtils.sendModMessage("No path loaded. Set a path key first.");
                return;
            }
            SavableBlockArea savableArea = new SavableBlockArea(area);
            SavableBlockAreaList loadedPath = path.path;
            loadedPath.add(savableArea);
            PlayerUtils.sendModMessage("Added checkpoint area.");
        });
        for (ArgumentBuilder<FabricClientCommandSource, ?> addCmd : addCmds) {
            addCommand.then(addCmd);
        }
        checkpointCommand.then(addCommand);

        RequiredArgumentBuilder<FabricClientCommandSource, Integer> insertCommand = argument("checkpoint", IntegerArgumentType.integer(1))
                .suggests((c, b) -> {
                    Path path = loadedPath();
                    if (path == null) return b.buildFuture();
                    int size = path.path.size();
                    for (int i = 0; i <= size; i++) {
                        b.suggest(i + 1);
                    }
                    return b.buildFuture();
                });

        List<ArgumentBuilder<FabricClientCommandSource, ?>> insertCmds = BlockArea.commands((context, area) -> {
            Path path = loadedPath();
            if (path == null) {
                PlayerUtils.sendModMessage("No path loaded. Set a path key first.");
                return;
            }
            int checkpointIndex = IntegerArgumentType.getInteger(context, "checkpoint") - 1;
            SavableBlockArea savableArea = new SavableBlockArea(area);
            SavableBlockAreaList loadedPath = path.path;
            loadedPath.add(checkpointIndex, savableArea);
            PlayerUtils.sendModMessage("Added checkpoint area.");

        });
        for (ArgumentBuilder<FabricClientCommandSource, ?> insertCmd : insertCmds) {
            insertCommand.then(insertCmd);
        }
        checkpointCommand.then(literal("insert").then(insertCommand));

        checkpointCommand.then(literal("list").executes(c -> {
            Path path = loadedPath();
            if (path == null) {
                PlayerUtils.sendModMessage("No path loaded. Set a path key first.");
                return 1;
            }
            SavableBlockAreaList loadedPath = path.path;
            if (loadedPath.isEmpty()) {
                PlayerUtils.sendModMessage("No checkpoint areas defined.");
            } else {
                PlayerUtils.sendModMessage("Checkpoint areas:");
                for (int i = 0; i < loadedPath.size(); i++) {
                    PlayerUtils.sendModMessage(" " + (i + 1) + ": " + loadedPath.get(i).toString());
                }
            }
            return 1;
        }));

        checkpointCommand.then(literal("clear").executes(c -> {
            Path path = loadedPath();
            if (path == null) {
                PlayerUtils.sendModMessage("No path loaded. Set a path key first.");
                return 1;
            }
            SavableBlockAreaList loadedPath = path.path;
            loadedPath.clear();
            PlayerUtils.sendModMessage("Cleared all checkpoint areas.");
            return 1;
        }));

        checkpointCommand.then(literal("remove").then(argument("index", IntegerArgumentType.integer()).executes(c -> {
            Path path = loadedPath();
            if (path == null) {
                PlayerUtils.sendModMessage("No path loaded. Set a path key first.");
                return 1;
            }
            SavableBlockAreaList loadedPath = path.path;
            int index = IntegerArgumentType.getInteger(c, "index") - 1;
            if (index < 0 || index >= loadedPath.size()) {
                PlayerUtils.sendModMessage("Invalid index. There are " + loadedPath.size() + " checkpoint areas.");
                return 1;
            }
            loadedPath.remove(index);
            PlayerUtils.sendModMessage("Removed checkpoint area " + (index + 1) + ".");
            return 1;
        })));

        checkpointCommand.then(literal("nearest").executes(c -> {
            Path path = loadedPath();
            if (path == null) {
                PlayerUtils.sendModMessage("No path loaded. Set a path key first.");
                return 1;
            }
            SavableBlockAreaList loadedPath = path.path;
            LocalPlayer player = PlayerUtils.getPlayer();
            if (loadedPath.isEmpty()) {
                PlayerUtils.sendModMessage("No checkpoint areas defined.");
                return 1;
            }
            SavableBlockArea nearestArea = null;
            int nearestIndex = -1;
            double nearestDistance = Double.MAX_VALUE;
            for (int i = 0; i < loadedPath.size(); i++) {
                SavableBlockArea area = loadedPath.get(i);
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

        return checkpointCommand;
    }

    private Path loadedPath() {
        if (loadedPathKey == null) {
            return null;
        }
        return savedPaths.computeIfAbsent(loadedPathKey, k -> new Path(new SavableBlockAreaList(), new PathTimesList()));
    }

    private void onRender(float deltaTick) {
        Path path = loadedPath();
        if (path == null) return;
        SavableBlockAreaList loadedPath = path.path;
        if (loadedPath == null) return;
        if (shouldRender) {
            renderLoadedPath();
        }

        if (!enabled || nextCheckpointIndex >= loadedPath.size()) return;
        checkCheckpointReached(loadedPath);
    }

    private void checkCheckpointReached(SavableBlockAreaList loadedPath) {
        //check for checkpoint reach
        BlockArea nextCheckpoint = loadedPath.get(nextCheckpointIndex);
        if (nextCheckpoint == null) return;
        LocalPlayer player = PlayerUtils.getPlayer();
        if (nextCheckpoint.contains(player.blockPosition())) {
            if (nextCheckpointIndex != 0) {
                long currentTime = System.currentTimeMillis();
                long totalTime = currentTime - startTime;
                long deltaTime = totalTime - (checkpointTimes.isEmpty() ? 0 : checkpointTimes.getLast());
                checkpointTimes.add(totalTime);
                Component timeComponent = formatTimeComponent(totalTime, deltaTime);
                PlayerUtils.sendModMessage(Component.text("Reached checkpoint " + (nextCheckpointIndex) + ". ", NamedTextColor.DARK_AQUA).append(timeComponent));
            } else {
                startTime = System.currentTimeMillis();
            }
            nextCheckpointIndex++;
            if (nextCheckpointIndex >= loadedPath.size()) {
                pathFinished();
            }
        }
    }

    private void pathFinished() {
        Path path = loadedPath();
        if (path == null) return;
        //save times
        LongList longList = new LongList();
        longList.addAll(checkpointTimes);
        PathTimes pathTimes = new PathTimes(longList, startTime);
        path.times.add(pathTimes);
        PlayerUtils.sendModMessage("End reached, times:");
        long previousTime = 0;
        for (int i = 0; i < checkpointTimes.size(); i++) {
            long checkpointTime = checkpointTimes.get(i);
            long splitTime = checkpointTime - previousTime;
            previousTime = checkpointTime;
            Component splitComponent = formatTimeComponent(checkpointTime, splitTime);
            PlayerUtils.sendMessage(Component.text(" Checkpoint " + (i + 1) + ": ", NamedTextColor.DARK_AQUA).append(splitComponent));
        }
        onEnable();
    }

    private void renderLoadedPath() {
        Path path = loadedPath();
        if (path == null) return;
        SavableBlockAreaList loadedPath = path.path;
        Entity cameraEntity = Minecraft.getInstance().getCameraEntity();
        if (cameraEntity == null) return;
        float xRot = cameraEntity.getXRot();
        float yRot = cameraEntity.getYRot();
        for (int i = 0; i < loadedPath.size(); i++) {
            SavableBlockArea area = loadedPath.get(i);
            Runnable renderer = rendererCache.computeIfAbsent(area, BlockAreaRenderFactory::createRenderTask);
            Vec3 center = area.center().getCenter();
            RenderUtils.drawTextPlate(List.of("" + (i + 1)), center.x(), center.y() + .25, center.z(), yRot, xRot, .05f, 0xFFFFFFFF, 0x40000000, false);
            renderer.run();
        }
    }

    private void onTick(LocalPlayer localPlayer) {
        Path path = loadedPath();
        if (path == null) return;
        SavableBlockAreaList loadedPath = path.path;
        //only used for actionbar messages, the timing is done on the render thread
        if (!enabled || loadedPath == null || nextCheckpointIndex == 0) return;
        long totalTime = System.currentTimeMillis() - startTime;
        long lastCheckpointTime = checkpointTimes.isEmpty() ? 0 : checkpointTimes.getLast();
        long deltaTime = totalTime - lastCheckpointTime;
        Component timeComponent = formatTimeComponent(totalTime, deltaTime);
        PlayerUtils.sendActionBar(Component.text("Path Timer: ").append(timeComponent));
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
