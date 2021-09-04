package at.haha007.edenclient.mods;

import at.haha007.edenclient.callbacks.ConfigLoadCallback;
import at.haha007.edenclient.callbacks.ConfigSaveCallback;
import at.haha007.edenclient.callbacks.GameRenderCallback;
import at.haha007.edenclient.callbacks.PlayerTickCallback;
import at.haha007.edenclient.command.CommandManager;
import at.haha007.edenclient.utils.PlayerUtils;
import at.haha007.edenclient.utils.RenderUtils;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.client.gl.VertexBuffer;
import net.minecraft.client.network.ClientCommandSource;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientChunkManager;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.*;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.chunk.WorldChunk;

import java.util.*;
import java.util.stream.Collectors;

import static at.haha007.edenclient.command.CommandManager.argument;
import static at.haha007.edenclient.command.CommandManager.literal;
import static at.haha007.edenclient.utils.PlayerUtils.sendModMessage;

public class TileEntityEsp {
    private boolean enabled;
    boolean tracer;
    int r, g, b, distance, maxCount;
    List<Vec3i> tileEntities = new ArrayList<>();
    Set<BlockEntityType<?>> types = new HashSet<>();
    private VertexBuffer wireframeBox;

    public TileEntityEsp() {
        GameRenderCallback.EVENT.register(this::render);
        PlayerTickCallback.EVENT.register(this::tick);
        ConfigLoadCallback.EVENT.register(this::load);
        ConfigSaveCallback.EVENT.register(this::save);
        registerCommand();
    }

    private void tick(ClientPlayerEntity player) {
        if (!enabled) {
            tileEntities = new ArrayList<>();
            return;
        }
        ChunkPos chunkPos = player.getChunkPos();
        ClientChunkManager cm = player.clientWorld.getChunkManager();
        BlockPos pp = player.getBlockPos();
        tileEntities = ChunkPos.stream(chunkPos, distance)
                .flatMap(cp -> {
                    WorldChunk wc = cm.getWorldChunk(cp.x, cp.z, false);
                    if (wc == null) return null;
                    return wc.getBlockEntities().entrySet().stream();
                })
                .filter(e -> types.contains(e.getValue().getType()))
                .map(Map.Entry::getKey)
                .sorted(Comparator.comparingDouble(pos -> pos.getSquaredDistance(pp)))
                .limit(maxCount)
                .map(v -> (Vec3i) v).toList();
    }

    private void load(NbtCompound nbtCompound) {
        wireframeBox = new VertexBuffer();
        Box bb = new Box(0, 0, 0, 1, 1, 1);
        RenderUtils.drawOutlinedBox(bb, wireframeBox);

        NbtCompound tag = nbtCompound.getCompound("tileEntityEsp");
        types = new HashSet<>();
        if (tag.isEmpty()) {
            enabled = false;
            r = g = b = 255;
            distance = 5;
            tracer = false;
            maxCount = 100;
            return;
        }
        enabled = tag.getBoolean("enabled");
        tracer = tag.getBoolean("tracer");

        r = tag.getInt("r");
        g = tag.getInt("g");
        b = tag.getInt("b");

        Registry<BlockEntityType<?>> registry = Registry.BLOCK_ENTITY_TYPE;
        for (NbtElement nbtElement : tag.getList("types", NbtElement.STRING_TYPE)) {
            types.add(registry.get(new Identifier(nbtElement.asString())));
        }

        distance = tag.getInt("distance");
        maxCount = tag.getInt("maxCount");
    }

    private void save(NbtCompound nbtCompound) {
        NbtCompound tag = new NbtCompound();
        tag.putBoolean("enabled", enabled);
        tag.putBoolean("tracer", tracer);

        tag.putInt("r", r);
        tag.putInt("g", g);
        tag.putInt("b", b);

        tag.putInt("maxCount", maxCount);
        tag.putInt("distance", distance);

        nbtCompound.put("tileEntityEsp", tag);

        Registry<BlockEntityType<?>> registry = Registry.BLOCK_ENTITY_TYPE;
        NbtList nbtTypeList = new NbtList();
        for (BlockEntityType<?> type : types) {
            nbtTypeList.add(NbtString.of(Objects.requireNonNull(registry.getId(type)).toString()));
        }
        tag.put("types", nbtTypeList);


        tileEntities = new ArrayList<>();
        wireframeBox.close();
        wireframeBox = null;
    }

    private void registerCommand() {
        LiteralArgumentBuilder<ClientCommandSource> cmd = literal("tileentityesp");
        LiteralArgumentBuilder<ClientCommandSource> toggle = literal("toggle");
        toggle.executes(c -> {
            enabled = !enabled;
            sendModMessage(enabled ? "TileEntityEsp enabled" : "TileEntityEsp disabled");
            return 1;
        });

        Registry<BlockEntityType<?>> registry = Registry.BLOCK_ENTITY_TYPE;

        for (BlockEntityType<?> type : registry) {
            toggle.then(literal(Objects.requireNonNull(registry.getId(type)).toString().replace("minecraft:", ""))
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
            sendModMessage(new LiteralText("Distance: ").formatted(Formatting.GOLD)
                    .append(new LiteralText(Integer.toString(distance)).formatted(Formatting.AQUA)));
            return 1;
        }).then(argument("dist", IntegerArgumentType.integer(1)).executes(c -> {
            distance = c.getArgument("dist", Integer.class);
            sendModMessage(new LiteralText("Distance: ").formatted(Formatting.GOLD)
                    .append(new LiteralText(Integer.toString(distance)).formatted(Formatting.AQUA)));
            return 1;
        })));

        cmd.then(literal("count").executes(c -> {
            sendModMessage(new LiteralText("Max count: ").formatted(Formatting.GOLD)
                    .append(new LiteralText(Integer.toString(maxCount)).formatted(Formatting.AQUA)));
            return 1;
        }).then(argument("count", IntegerArgumentType.integer(1)).executes(c -> {
            maxCount = c.getArgument("count", Integer.class);
            sendModMessage(new LiteralText("Max count: ").formatted(Formatting.GOLD)
                    .append(new LiteralText(Integer.toString(maxCount)).formatted(Formatting.AQUA)));
            return 1;
        })));

        cmd.then(literal("list").executes(c -> {
            String str = types.stream()
                    .map(Registry.BLOCK_ENTITY_TYPE::getId)
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
        CommandManager.register(cmd);
    }

    RequiredArgumentBuilder<ClientCommandSource, Integer> arg(String key) {
        return argument(key, IntegerArgumentType.integer(0, 255));
    }

    private int setColor(CommandContext<ClientCommandSource> c) {
        this.r = c.getArgument("r", Integer.class);
        this.g = c.getArgument("g", Integer.class);
        this.b = c.getArgument("b", Integer.class);
        sendModMessage(new LiteralText("Color updated.").formatted(Formatting.GOLD));
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
            Matrix4f matrix = matrixStack.peek().getModel();
            BufferBuilder bb = Tessellator.getInstance().getBuffer();

            bb.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION);
            for (Vec3i t : tileEntities) {
                bb.vertex(matrix, t.getX(), t.getY(), t.getZ()).next();
                bb.vertex(matrix, start.getX(), start.getY(), start.getZ()).next();
            }
            bb.end();
            BufferRenderer.draw(bb);
            matrixStack.pop();
        }

        tileEntities.forEach(c -> {
            matrixStack.push();
            matrixStack.translate(c.getX(), c.getY(), c.getZ());
            this.wireframeBox.setShader(matrixStack.peek().getModel(), RenderSystem.getProjectionMatrix(), RenderSystem.getShader());
            matrixStack.pop();
        });
    }
}
