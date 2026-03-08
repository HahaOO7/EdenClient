package at.haha007.edenclient.mods;

import at.haha007.edenclient.EdenClient;
import at.haha007.edenclient.annotations.Mod;
import at.haha007.edenclient.callbacks.ContainerCloseCallback;
import at.haha007.edenclient.callbacks.PlayerBreakBlockCallback;
import at.haha007.edenclient.callbacks.PlayerTickCallback;
import at.haha007.edenclient.callbacks.UpdateLevelChunkCallback;
import at.haha007.edenclient.command.CommandManager;
import at.haha007.edenclient.mods.datafetcher.ContainerInfo;
import at.haha007.edenclient.mods.datafetcher.DataFetcher;
import at.haha007.edenclient.utils.PlayerUtils;
import at.haha007.edenclient.utils.config.ConfigSubscriber;
import at.haha007.edenclient.utils.config.PerWorldConfig;
import at.haha007.edenclient.utils.tasks.SyncTask;
import at.haha007.edenclient.utils.tasks.TaskManager;
import at.haha007.edenclient.utils.tasks.WaitForTicksTask;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.xpple.clientarguments.arguments.CBlockPosArgument;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.TranslatableComponent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static at.haha007.edenclient.command.CommandManager.argument;
import static at.haha007.edenclient.command.CommandManager.literal;
import static at.haha007.edenclient.utils.PlayerUtils.sendModMessage;

@Mod(dependencies = DataFetcher.class)
public class ContainerDisplay {
    private List<Integer> displayEntityIds = new ArrayList<>();
    @ConfigSubscriber("false")
    private boolean enabled;
    private Map<Vec3i, ContainerInfo.ChestInfo> entries = new ConcurrentHashMap<>();
    private ChunkPos lastChunkPos = new ChunkPos(0, 0);
    private boolean shouldUpdate = true;

    public ContainerDisplay() {
        PlayerTickCallback.EVENT.register(this::tick, getClass());
        ContainerCloseCallback.EVENT.register(t -> shouldUpdate = true, getClass());
        UpdateLevelChunkCallback.EVENT.register(c -> updateLater(), getClass());
        PlayerBreakBlockCallback.EVENT.register((a, b, c) -> updateLater(), getClass());
        PerWorldConfig.get().register(this, "ContainerDisplay");
        WorldRenderEvents.AFTER_ENTITIES.register(this::render);
        registerCommand();
    }

    private void render(WorldRenderContext context) {
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        Minecraft mc = Minecraft.getInstance();
        PoseStack poseStack = context.matrices();
        poseStack.pushPose();
        Vec3 cam = mc.gameRenderer.getMainCamera().position();
        Entity camera = Minecraft.getInstance().getCameraEntity();
        if (camera == null) {
            return;
        }

        entries.forEach((pos, chestInfo) -> {
            if (chestInfo == null || chestInfo.items().isEmpty()) {
                return;
            }
            poseStack.pushPose();
            Vec3 blockVec = Vec3.atLowerCornerOf(pos);
            poseStack.translate(
                    blockVec.subtract(cam)
            );
            //calculate looking direction, rendering offset and rendering angle
            final Direction direction = chestInfo.face();
            final Vec3 offset = Vec3.atLowerCornerOf(direction.getUnitVec3i().offset(1, 1, 1)).scale(.5);

            final Quaternionf rotation = direction.getRotation();
            if (direction.getAxis() == Direction.Axis.Y) {
                Quaternionf horizontal = camera.getDirection().getRotation();
                horizontal.mul(Direction.NORTH.getRotation());
                rotation.mul(horizontal);
                rotation.mul(Direction.NORTH.getRotation());
            } else {
                rotation.mul(Direction.EAST.getRotation());
                rotation.rotateZ(-1.5707964f);
            }
            List<Item> items = new ArrayList<>(chestInfo.items());

            int loopCount = Math.min(items.size(), 9);

            if (loopCount > 1) {
                //multiple items -> render 3x3 items
                int columns = 3;
                float spacing = 0.28f;

                // Compute total width/height so we can center the grid
                int rows = (loopCount + columns - 1) / columns;

                float totalWidth = (columns - 1) * spacing;
                float totalHeight = (rows - 1) * spacing;

                float halfWidth = totalWidth / 2f;
                float halfHeight = totalHeight / 2f;

                for (int i = 0; i < loopCount; i++) {
                    ItemStack stack = items.get(i).getDefaultInstance();
                    // Grid coordinates
                    int col = i % columns;
                    int row = i / columns;

                    // Local offsets, centered around (0,0)
                    float x = (col * spacing) - halfWidth;     // left to right
                    float y = halfHeight - (row * spacing);    // top to bottom

                    Vec3 delta;

                    // Convert grid (x,y) into block-face relative 3D offsets
                    switch (direction) {
                        case DOWN, UP -> delta = new Vec3(x, 0, y);
                        case NORTH -> delta = new Vec3(-x, y, 0);
                        case SOUTH -> delta = new Vec3(x, y, 0);
                        case WEST -> delta = new Vec3(0, y, x);
                        case EAST -> delta = new Vec3(0, y, -x);
                        default -> delta = new Vec3(x, 0, y);
                    }

                    poseStack.pushPose();
                    poseStack.translate(delta.add(offset));
                    poseStack.mulPose(rotation);
                    poseStack.scale(0.4f, 0.4f, 0.4f);
                    ItemStackRenderState state = new ItemStackRenderState();
                    mc.getItemModelResolver().updateForTopItem(
                            state,
                            stack,
                            ItemDisplayContext.FIXED,
                            mc.level,
                            mc.player,
                            255
                    );
                    SubmitNodeCollector collector = context.commandQueue();
                    state.submit(
                            poseStack,
                            collector,
                            LightTexture.FULL_BRIGHT,
                            OverlayTexture.NO_OVERLAY,
                            0
                    );
                    poseStack.popPose();

                }
            } else if (loopCount == 1) {
                poseStack.pushPose();
                poseStack.translate(offset);
                poseStack.mulPose(rotation);
                ItemStack stack = chestInfo.items().getFirst().getDefaultInstance();
                ItemStackRenderState state = new ItemStackRenderState();
                mc.getItemModelResolver().updateForTopItem(
                        state,
                        stack,
                        ItemDisplayContext.FIXED,
                        mc.level,
                        mc.player,
                        255
                );
                SubmitNodeCollector collector = context.commandQueue();
                state.submit(
                        poseStack,
                        collector,
                        LightTexture.FULL_BRIGHT,
                        OverlayTexture.NO_OVERLAY,
                        0
                );
                poseStack.popPose();
            } else {
                throw new IllegalStateException("Invalid loop Count!");
            }

            poseStack.popPose();
        });

        poseStack.popPose();
    }

    private void updateLater() {
        TaskManager tm = new TaskManager();
        tm.then(new WaitForTicksTask(1));
        tm.then(() -> shouldUpdate = true);
        tm.start();
    }

    private void tick(LocalPlayer ignore) {
        Entity camera = Minecraft.getInstance().getCameraEntity();
        if (camera == null) {
            return;
        }

        update(camera.blockPosition(), camera.chunkPosition());
        if (!enabled) {
            return;
        }
        ChunkPos chunkPos = camera.chunkPosition();
        if (!chunkPos.equals(this.lastChunkPos)) {
            shouldUpdate = true;
        }
        this.lastChunkPos = chunkPos;
    }

    private void update(BlockPos pp, ChunkPos chunkPos) {
        if (!shouldUpdate) return;
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return;
        //clear old entities
        TaskManager tm = new TaskManager();
        List<Integer> oldDisplayEntities = displayEntityIds;
        displayEntityIds = new ArrayList<>();
        tm.then(new WaitForTicksTask(1));
        tm.then(new SyncTask(() -> {
            for (Integer id : oldDisplayEntities) {
                level.removeEntity(id, Entity.RemovalReason.DISCARDED);
            }
        }));
        tm.start();

        if (!enabled) return;
        //find nearby containers to create displays for
        entries = new ConcurrentHashMap<>();
        ChunkPos.rangeClosed(chunkPos, 2).forEach(cp -> {
            Map<Vec3i, ContainerInfo.ChestInfo> info = EdenClient.getMod(DataFetcher.class).getContainerInfo().getContainerInfo(cp);
            if (info != null) {
                entries.putAll(info);
            }
        });
        entries = entries.entrySet().stream()
                .filter(e -> level.getBlockEntity(new BlockPos(e.getKey())) != null)
                .sorted(Comparator.comparingInt(e -> e.getKey().distManhattan(pp)))
                .limit(250)
                .collect(Collectors.toConcurrentMap(Map.Entry::getKey, Map.Entry::getValue));

        shouldUpdate = false;
    }

    private void registerCommand() {
        LiteralArgumentBuilder<FabricClientCommandSource> cmd = literal("econtainerdisplay");
        LiteralArgumentBuilder<FabricClientCommandSource> toggle = literal("toggle");
        toggle.executes(c -> {
            enabled = !enabled;
            shouldUpdate = true;
            sendModMessage(enabled ? "ContainerDisplay enabled" : "ContainerDisplay disabled");
            return 1;
        });

        cmd.then(literal("clear").executes(c -> {
            entries.clear();
            sendModMessage("Cleared cached containers.");
            return 1;
        }));

        cmd.then(literal("debug").then(argument("pos", CBlockPosArgument.blockPos()).executes(c -> {
            BlockPos blockPos = CBlockPosArgument.getBlockPos(c, "pos");
            ChunkPos chunkPos = new ChunkPos(blockPos);
            ContainerInfo.ChestInfo chestInfo = EdenClient.getMod(DataFetcher.class)
                    .getContainerInfo()
                    .getContainerInfo(chunkPos)
                    .get(blockPos);

            PlayerUtils.sendModMessage("Items in container:");
            List<TranslatableComponent> texts = chestInfo.items().stream()
                    .map(Item::getDescriptionId)
                    .map(Component::translatable).toList();
            Component joined = Component.join(JoinConfiguration.builder().separator(Component.text(", ")).build(), texts);
            PlayerUtils.sendMessage(joined);
            return 1;
        })));

        cmd.then(toggle);
        CommandManager.register(cmd, "Displays icons on top of containers.");
    }

}
