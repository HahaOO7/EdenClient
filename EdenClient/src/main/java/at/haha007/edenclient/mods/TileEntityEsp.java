package at.haha007.edenclient.mods;

import at.haha007.edenclient.Mod;
import at.haha007.edenclient.callbacks.GameRenderCallback;
import at.haha007.edenclient.callbacks.JoinWorldCallback;
import at.haha007.edenclient.callbacks.LeaveWorldCallback;
import at.haha007.edenclient.callbacks.PlayerTickCallback;
import at.haha007.edenclient.utils.ChatColor;
import at.haha007.edenclient.utils.PlayerUtils;
import at.haha007.edenclient.utils.RenderUtils;
import at.haha007.edenclient.utils.config.ConfigSubscriber;
import at.haha007.edenclient.utils.config.PerWorldConfig;
import at.haha007.edenclient.utils.config.wrappers.BlockEntityTypeSet;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.client.multiplayer.ClientChunkCache;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.*;
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
    float r, g, b;
    @ConfigSubscriber("1000")
    int distance;
    @ConfigSubscriber("1000")
    int maxCount;
    @ConfigSubscriber("chest")
    BlockEntityTypeSet types = new BlockEntityTypeSet();
    List<Vec3i> tileEntities = new ArrayList<>();
    private VertexBuffer wireframeBox;

    public TileEntityEsp() {
        GameRenderCallback.EVENT.register(this::render);
        PlayerTickCallback.EVENT.register(this::tick);
        JoinWorldCallback.EVENT.register(this::build);
        LeaveWorldCallback.EVENT.register(this::destroy);
        PerWorldConfig.get().register(this, "tileEntityEsp");
        registerCommand();
    }

    private void tick(LocalPlayer player) {
        if (!enabled) {
            tileEntities = new ArrayList<>();
            return;
        }
        ChunkPos chunkPos = player.chunkPosition();
        ClientChunkCache cm = player.clientLevel.getChunkSource();
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

    private void build() {
        wireframeBox = new VertexBuffer(VertexBuffer.Usage.STATIC);
        AABB bb = new AABB(0, 0, 0, 1, 1, 1);
        RenderUtils.drawOutlinedBox(bb, wireframeBox);
    }

    private void destroy() {
        tileEntities = new ArrayList<>();
        wireframeBox.close();
    }

    private void registerCommand() {
        LiteralArgumentBuilder<ClientSuggestionProvider> cmd = literal("etileentityesp");
        LiteralArgumentBuilder<ClientSuggestionProvider> toggle = literal("toggle");
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
            sendModMessage(ChatColor.GOLD + "Distance: " + ChatColor.AQUA + distance);
            return 1;
        }).then(argument("dist", IntegerArgumentType.integer(1)).executes(c -> {
            distance = c.getArgument("dist", Integer.class);
            sendModMessage(ChatColor.GOLD + "Distance: " + ChatColor.AQUA + distance);
            return 1;
        })));

        cmd.then(literal("count").executes(c -> {
            sendModMessage(ChatColor.GOLD + "Max count: " + ChatColor.AQUA + maxCount);
            return 1;
        }).then(argument("count", IntegerArgumentType.integer(1)).executes(c -> {
            maxCount = c.getArgument("count", Integer.class);
            sendModMessage(ChatColor.GOLD + "Max count: " + ChatColor.AQUA + maxCount);
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

        cmd.then(literal("color").then(arg("r").then(arg("g").then(arg("b").executes(this::setColor)))));

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
            for (Vec3i t : tileEntities) {
                bb.vertex(matrix, t.getX(), t.getY(), t.getZ()).endVertex();
                bb.vertex(matrix, start.x(), start.y(), start.z()).endVertex();
            }
            BufferUploader.drawWithShader(Objects.requireNonNull(bb.end()));
            matrixStack.popPose();
        }

        tileEntities.forEach(c -> {
            matrixStack.pushPose();
            matrixStack.translate(c.getX(), c.getY(), c.getZ());
            wireframeBox.bind();
            wireframeBox.drawWithShader(matrixStack.last().pose(), RenderSystem.getProjectionMatrix(), RenderSystem.getShader());
            VertexBuffer.unbind();
            matrixStack.popPose();
        });
    }
}
