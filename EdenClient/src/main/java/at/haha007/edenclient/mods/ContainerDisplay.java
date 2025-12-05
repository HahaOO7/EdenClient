package at.haha007.edenclient.mods;

import at.haha007.edenclient.EdenClient;
import at.haha007.edenclient.annotations.Mod;
import at.haha007.edenclient.callbacks.ContainerCloseCallback;
import at.haha007.edenclient.callbacks.PlayerTickCallback;
import at.haha007.edenclient.command.CommandManager;
import at.haha007.edenclient.mods.datafetcher.ContainerInfo;
import at.haha007.edenclient.mods.datafetcher.DataFetcher;
import at.haha007.edenclient.utils.config.ConfigSubscriber;
import at.haha007.edenclient.utils.config.PerWorldConfig;
import at.haha007.edenclient.utils.tasks.SyncTask;
import at.haha007.edenclient.utils.tasks.TaskManager;
import at.haha007.edenclient.utils.tasks.WaitForTicksTask;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.math.Transformation;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.util.Brightness;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static at.haha007.edenclient.command.CommandManager.literal;
import static at.haha007.edenclient.utils.PlayerUtils.getPlayer;
import static at.haha007.edenclient.utils.PlayerUtils.sendModMessage;

@Mod(dependencies = DataFetcher.class)
public class ContainerDisplay {
    private List<Integer> displayEntityIds = new ArrayList<>();
    @ConfigSubscriber("true")
    private boolean enabled;
    private Map<Vec3i, ContainerInfo.ChestInfo> entries = new ConcurrentHashMap<>();
    private ChunkPos lastChunkPos = new ChunkPos(0, 0);
    private boolean shouldUpdate = true;

    public ContainerDisplay() {
        PlayerTickCallback.EVENT.register(this::tick, getClass());
        ContainerCloseCallback.EVENT.register(t -> shouldUpdate = true, getClass());
        PerWorldConfig.get().register(this, "ContainerDisplay");
        registerCommand();
    }

    private void tick(LocalPlayer player) {
        update(player.blockPosition(), player.chunkPosition());
        if (!enabled) {
            return;
        }
        ChunkPos chunkPos = player.chunkPosition();
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
        ChunkPos.rangeClosed(chunkPos, 1).forEach(cp -> {
            Map<Vec3i, ContainerInfo.ChestInfo> info = EdenClient.getMod(DataFetcher.class).getContainerInfo().getContainerInfo(cp);
            if (info != null) {
                entries.putAll(info);
            }
        });
        entries = entries.entrySet().stream()
                .sorted(Comparator.comparingInt(e -> e.getKey().distManhattan(pp)))
                .limit(200)
                .collect(Collectors.toConcurrentMap(Map.Entry::getKey, Map.Entry::getValue));

        //update display entities
        createDisplayEntities();
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

        cmd.then(literal("test").executes(c -> {
            createDisplayEntities();
            return 1;
        }));

        cmd.then(toggle);
        CommandManager.register(cmd, "Displays icons on top of containers.");
    }


    private void createDisplayEntities() {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return;
        GL11.glEnable(GL11.GL_DEPTH_TEST);

        entries.forEach((pos, chestInfo) -> {
            BlockPos blockPos = new BlockPos(pos);
            BlockState state = level.getBlockState(blockPos);

            //calculate looking direction, rendering offset and rendering angle
            final Direction direction = chestInfo.face();
            final Vec3 offset = Vec3.atLowerCornerOf(direction.getUnitVec3i().offset(1, 1, 1)).scale(.5);

            final Quaternionf rotation = direction.getRotation();
            if (direction.getAxis() == Direction.Axis.Y) {
                Quaternionf horizontal = getPlayer().getDirection().getRotation();
                horizontal.mul(Direction.NORTH.getRotation());
                rotation.mul(horizontal);
                rotation.mul(Direction.NORTH.getRotation());
            } else {
                rotation.mul(Direction.EAST.getRotation());
                rotation.rotateZ(-1.5707964f);
            }

            // Decide which items to render:
            // - single chest / non-chest: use its own items
            // - double chest + outer side (left/right): merge both halves (full content)
            // - double chest + front/back/top/bottom: use its own (split) items
            List<Item> items = new ArrayList<>(chestInfo.items());

            if (state.getBlock() instanceof ChestBlock chestBlock) {
                ChestType type = state.getValue(ChestBlock.TYPE);
                if (type != ChestType.SINGLE) {
                    Direction facing = state.getValue(ChestBlock.FACING);
                    Direction side1 = facing.getClockWise();
                    Direction side2 = facing.getCounterClockWise();

                    boolean isOuterSide = (direction == side1 || direction == side2);

                    if (isOuterSide) {
                        // Merge items from both halves to show full content on outer sides
                        Direction connectedDir = ChestBlock.getConnectedDirection(state);
                        BlockPos otherPos = blockPos.relative(connectedDir);
                        ContainerInfo.ChestInfo otherInfo = entries.get(otherPos);
                        if (otherInfo != null) {
                            for (Item it : otherInfo.items()) {
                                if (!items.contains(it)) {
                                    items.add(it);
                                }
                            }
                        }
                    }
                }
            }

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
                    Item item = items.get(i);

                    // Grid coordinates
                    int col = i % columns;
                    int row = i / columns;

                    // Local offsets, centered around (0,0)
                    float x = (col * spacing) - halfWidth;     // left → right
                    float y = halfHeight - (row * spacing);    // top → bottom

                    float dx;
                    float dy;
                    float dz;

                    // Convert grid (x,y) into block-face relative 3D offsets
                    if (direction.getAxis() == Direction.Axis.X) {
                        dx = 0;
                        dy = y;
                        dz = x;

                    } else if (direction.getAxis() == Direction.Axis.Y) {
                        dx = x;
                        dy = 0;
                        dz = y;

                    } else { // Z axis
                        dx = x;
                        dy = y;
                        dz = 0;
                    }

                    Display.ItemDisplay display =
                            new Display.ItemDisplay(EntityType.ITEM_DISPLAY, level);

                    display.setPos(pos.getX() + offset.x(),
                            pos.getY() + offset.y(),
                            pos.getZ() + offset.z());

                    Vector3f offsetPos = new Vector3f(dx, dy, dz);
                    Vector3f scale = new Vector3f(.22f, .22f, .22f);
                    Transformation transform = new Transformation(offsetPos, new Quaternionf(), scale, rotation);
                    display.setTransformation(transform);
                    display.setBrightnessOverride(Brightness.FULL_BRIGHT);
                    display.setItemStack(item.getDefaultInstance());
                    level.addEntity(display);
                    displayEntityIds.add(display.getId());
                }
            } else if (loopCount == 1) {
                //one item -> render it BIG!
                Item item = items.getFirst();

                Display.ItemDisplay display = new Display.ItemDisplay(EntityType.ITEM_DISPLAY, level);
                display.setPos(pos.getX() + offset.x(), pos.getY() + offset.y(), pos.getZ() + offset.z());
                Vector3f offsetPos = new Vector3f();
                Vector3f scale = new Vector3f(.6f, .6f, .6f);
                Transformation transformation = new Transformation(offsetPos, rotation, scale, new Quaternionf());
                display.setTransformation(transformation);
                display.setItemStack(item.getDefaultInstance());
                level.addEntity(display);
                display.setBrightnessOverride(Brightness.FULL_BRIGHT);
                displayEntityIds.add(display.getId());
            }
        });
    }
}
