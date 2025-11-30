package at.haha007.edenclient.mods;

import at.haha007.edenclient.annotations.Mod;
import at.haha007.edenclient.callbacks.GameRenderCallback;
import at.haha007.edenclient.callbacks.LeaveWorldCallback;
import at.haha007.edenclient.callbacks.PlayerTickCallback;
import at.haha007.edenclient.utils.EdenRenderUtils;
import at.haha007.edenclient.utils.config.ConfigSubscriber;
import at.haha007.edenclient.utils.config.PerWorldConfig;
import at.haha007.edenclient.utils.config.wrappers.BlockEntityTypeSet;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import fi.dy.masa.malilib.util.data.Color4f;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientChunkCache;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

import static at.haha007.edenclient.command.CommandManager.*;
import static at.haha007.edenclient.utils.PlayerUtils.sendModMessage;

@Mod
public class TileEntityEsp {
    @ConfigSubscriber("false")
    private boolean enabled;
    @ConfigSubscriber("true")
    boolean tracer;
    @ConfigSubscriber("1")
    float red;
    @ConfigSubscriber("1")
    float green;
    @ConfigSubscriber("1")
    float blue;
    @ConfigSubscriber("1000")
    int distance;
    @ConfigSubscriber("1000")
    int maxCount;
    @ConfigSubscriber("chest")
    BlockEntityTypeSet types = new BlockEntityTypeSet();
    List<Vec3i> tileEntities = new ArrayList<>();

    public TileEntityEsp() {
        GameRenderCallback.EVENT.register(this::render, getClass());
        PlayerTickCallback.EVENT.register(this::tick, getClass());
        LeaveWorldCallback.EVENT.register(this::destroy, getClass());
        PerWorldConfig.get().register(this, "tileEntityEsp");
        registerCommand();
    }

    private void tick(LocalPlayer player) {
        if (!enabled) {
            tileEntities = new ArrayList<>();
            return;
        }
        ChunkPos chunkPos = player.chunkPosition();
        ClientChunkCache cm = Minecraft.getInstance().level.getChunkSource();
        BlockPos pp = player.blockPosition();
        tileEntities = ChunkPos.rangeClosed(chunkPos, distance)
                .flatMap(cp -> {
                    LevelChunk wc = cm.getChunk(cp.x, cp.z, false);
                    if (wc == null) return null;
                    return wc.getBlockEntities().entrySet().stream();
                })
                .filter(e -> types.contains(e.getValue().getType()))
                .map(Map.Entry::getKey)
                .sorted(Comparator.comparingDouble(pos -> pos.distSqr(pp)))
                .limit(maxCount)
                .map(v -> (Vec3i) v).toList();
    }

    private void destroy() {
        tileEntities = new ArrayList<>();
    }

    private void registerCommand() {
        LiteralArgumentBuilder<FabricClientCommandSource> cmd = literal("etileentityesp");
        LiteralArgumentBuilder<FabricClientCommandSource> toggle = literal("toggle");
        toggle.executes(c -> {
            enabled = !enabled;
            sendModMessage(enabled ? "TileEntityEsp enabled" : "TileEntityEsp disabled");
            return 1;
        });

        Registry<BlockEntityType<?>> registry = BuiltInRegistries.BLOCK_ENTITY_TYPE;

        for (BlockEntityType<?> type : registry) {
            toggle.then(literal(Objects.requireNonNull(registry.getKey(type)).toString().replace("minecraft:", ""))
                    .executes(c -> {
                        if (types.contains(type)) {
                            types.remove(type);
                            sendModMessage("TileEntityType removed");
                        } else {
                            types.add(type);
                            sendModMessage("TileEntityType added");
                        }
                        return 1;
                    }));
        }

        cmd.then(literal("tracer").executes(c -> {
            tracer = !tracer;
            sendModMessage(tracer ? "Tracer enabled" : "Tracer disabled");
            return 1;
        }));

        cmd.then(literal("distance").executes(c -> {
            sendModMessage(Component.text("Distance: ", NamedTextColor.GOLD)
                    .append(Component.text(distance, NamedTextColor.AQUA)));
            return 1;
        }).then(argument("dist", IntegerArgumentType.integer(1)).executes(c -> {
            distance = c.getArgument("dist", Integer.class);
            sendModMessage(Component.text("Distance: ", NamedTextColor.GOLD)
                    .append(Component.text(distance, NamedTextColor.AQUA)));
            return 1;
        })));

        cmd.then(literal("count").executes(c -> {
            sendModMessage(Component.text("Max count: ", NamedTextColor.GOLD)
                    .append(Component.text(maxCount, NamedTextColor.AQUA)));

            return 1;
        }).then(argument("count", IntegerArgumentType.integer(1)).executes(c -> {
            maxCount = c.getArgument("count", Integer.class);
            sendModMessage(Component.text("Max count: ", NamedTextColor.GOLD)
                    .append(Component.text(maxCount, NamedTextColor.AQUA)));
            return 1;
        })));

        cmd.then(literal("list").executes(c -> {
            String str = types.stream()
                    .map(BuiltInRegistries.BLOCK_ENTITY_TYPE::getKey)
                    .map(String::valueOf)
                    .map(s -> s.substring(10))
                    .collect(Collectors.joining(", "));
            sendModMessage(str);
            return 1;
        }));

        cmd.then(literal("tracer").executes(c -> {
            tracer = !tracer;
            sendModMessage(tracer ? "Tracer enabled" : "Tracer disabled");
            return 1;
        }));

        cmd.then(literal("color").then(arg("r").then(arg("g").then(arg("b")
                .executes(this::setColor)))));

        cmd.then(literal("clear").executes(c -> {
            types.clear();
            sendModMessage("Cleared rendered types");
            return 1;
        }));

        cmd.then(toggle);
        register(cmd,
                "TileEntityEsp allows for all tile-entities of any specific type(s) to be surrounded by x-ray bounding boxes.",
                "It is also possible to enable tracers and to switch between solid/transparent rendering.");
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


    private void render(float v) {
        if (!enabled) return;
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        if (tracer && !tileEntities.isEmpty()) {
            EdenRenderUtils.drawTracers(tileEntities.stream().map(Vec3::atCenterOf).toList(), Color4f.fromColor(new Color(red, green, blue).getRGB()));
        }

        tileEntities.forEach(c -> EdenRenderUtils.drawAreaOutline(
                Vec3.atLowerCornerOf(c),
                Vec3.atLowerCornerOf(c.offset(1, 1, 1)),
                Color4f.fromColor(new Color(red, green, blue).getRGB())));
    }
}
