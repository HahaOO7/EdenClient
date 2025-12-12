package at.haha007.edenclient.mods.datafetcher;

import at.haha007.edenclient.EdenClient;
import at.haha007.edenclient.callbacks.*;
import at.haha007.edenclient.utils.PlayerUtils;
import at.haha007.edenclient.utils.Scheduler;
import at.haha007.edenclient.utils.config.ConfigSubscriber;
import at.haha007.edenclient.utils.config.PerWorldConfig;
import at.haha007.edenclient.utils.config.loaders.ConfigLoader;
import at.haha007.edenclient.utils.config.wrappers.ItemList;
import at.haha007.edenclient.utils.tasks.MaxTimeTask;
import at.haha007.edenclient.utils.tasks.SyncTask;
import at.haha007.edenclient.utils.tasks.TaskManager;
import at.haha007.edenclient.utils.tasks.WaitForInventoryTask;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.logging.LogUtils;
import lombok.Getter;
import lombok.experimental.Accessors;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.Vec3i;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.ShulkerBoxMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

@Accessors(fluent = true)
public class ContainerInfo {
    @ConfigSubscriber()
    @Getter
    private final ChunkChestMap chunkMap;
    private BlockPos lastInteractedBlock = null;
    private Direction lastClickedDirection = null;

    private AutoMode autoMode = null;
    TaskManager autoUpdateTask;
    private final Map<BlockPos, Long> updatedBlocks = new HashMap<>();


    ContainerInfo() {
        chunkMap = new ChunkChestMap();

        PlayerAttackBlockCallback.EVENT.register(this::attackBlock, getClass());
        PlayerInteractBlockCallback.EVENT.register(this::interactBlock, getClass());
        ContainerCloseCallback.EVENT.register(itemStacks -> {
            updateInventory(itemStacks);
            lastInteractedBlock = null;
        }, getClass());
        PlayerBreakBlockCallback.EVENT.register((a,b,c) -> {
            lastInteractedBlock = b;
            updateInventory(Collections.emptyList());
            lastInteractedBlock = null;
        }, getClass());
        PlayerInvChangeCallback.EVENT.register(this::onInventoryChange, getClass());
        UpdateLevelChunkCallback.EVENT.register(this::updateChunk, getClass());
        PlayerTickCallback.EVENT.register(this::tick, getClass());
        ContainerOpenCallback.EVENT.register(this::shouldCancelContainerOpen, getClass());
        LeaveWorldCallback.EVENT.register(this::destroy, getClass());

        PerWorldConfig.get().register(new ContainerConfigLoader(), ChunkChestMap.class);
        PerWorldConfig.get().register(new ChestMapLoader(), ChestMap.class);
        PerWorldConfig.get().register(new ChestInfoLoader(), ChestInfo.class);
        PerWorldConfig.get().register(this, "ContainerInfo");
    }

    private void destroy() {
        autoMode = null;
        if (autoUpdateTask != null)
            autoUpdateTask.cancel();
        autoUpdateTask = null;
    }

    private boolean shouldCancelContainerOpen(MenuType<?> type, int id, Component title) {
        return autoUpdateTask != null;
    }

    public LiteralArgumentBuilder<FabricClientCommandSource> registerCommand() {
        LiteralArgumentBuilder<FabricClientCommandSource> cmd = literal("chestinfo");
        cmd.then(literal("empty").executes(c -> {
            if (autoMode != null) {
                PlayerUtils.sendModMessage("AutoUpdate already running");
                return 1;
            }
            autoMode = AutoMode.EMPTY;
            updatedBlocks.clear();
            PlayerUtils.sendModMessage("AutoUpdate enabled");
            return 1;
        }));
        cmd.then(literal("old").executes(c -> {
            if (autoMode != null) {
                PlayerUtils.sendModMessage("AutoUpdate already running");
                return 1;
            }
            autoMode = AutoMode.OLD;
            updatedBlocks.clear();
            PlayerUtils.sendModMessage("AutoUpdate enabled");
            return 1;
        }));
        cmd.then(literal("old_empty").executes(c -> {
            if (autoMode != null) {
                PlayerUtils.sendModMessage("AutoUpdate already running");
                return 1;
            }
            autoMode = AutoMode.OLD_EMPTY;
            updatedBlocks.clear();
            PlayerUtils.sendModMessage("AutoUpdate enabled");
            return 1;
        }));
        cmd.then(literal("all").executes(c -> {
            if (autoMode != null) {
                PlayerUtils.sendModMessage("AutoUpdate already running");
                return 1;
            }
            autoMode = AutoMode.ALL;
            updatedBlocks.clear();
            PlayerUtils.sendModMessage("AutoUpdate enabled");
            return 1;
        }));
        cmd.then(literal("stop").executes(c -> {
            if (autoMode == null) {
                PlayerUtils.sendModMessage("AutoUpdate not running");
                return 1;
            }
            autoMode = null;
            if (autoUpdateTask != null)
                autoUpdateTask.cancel();
            autoUpdateTask = null;
            PlayerUtils.sendModMessage("AutoUpdate disabled");
            return 1;
        }));
        return cmd;
    }


    private void tick(LocalPlayer player) {
        if (autoMode == null) return;
        if (autoUpdateTask != null) return;

        //find nearby container that isnt already updated
        ClientLevel world = Minecraft.getInstance().level;
        if (world == null) return;
        Vec3 playerEyePosition = player.getEyePosition();
        double range = player.blockInteractionRange() + 1;
        Vec3 min = playerEyePosition.subtract(range, range, range);
        Vec3 max = playerEyePosition.add(range, range, range);
        BlockPos minBlockPos = new BlockPos((int) min.x, (int) min.y, (int) min.z);
        BlockPos maxBlockPos = new BlockPos((int) max.x, (int) max.y, (int) max.z);

        Predicate<BlockPos> filter = switch (autoMode) {
            case EMPTY -> p -> matchesEmpty(p, world);
            case ALL -> p -> !updatedBlocks.containsKey(p);
            case OLD -> p -> matchesOld(p, 60);
            case OLD_EMPTY -> p -> matchesOld(p, 60) && matchesEmpty(p, world);
        };
        if (autoMode == AutoMode.OLD || autoMode == AutoMode.OLD_EMPTY) {
            removeOldEntriesFromUpdatedBlocks(300);
        }

        Optional<BlockPos> closest = BlockPos.betweenClosedStream(minBlockPos, maxBlockPos)
                .map(BlockPos::new)
                .filter(p -> Vec3.atCenterOf(p).subtract(playerEyePosition).lengthSqr() < range * range)
                .filter(p -> world.getBlockEntity(p) != null)
                .filter(p -> world.getBlockEntity(p) instanceof BaseContainerBlockEntity)
                .filter(filter)
                .min(Comparator.comparingDouble(p -> Vec3.atCenterOf(p).subtract(playerEyePosition).lengthSqr()));
        if (closest.isEmpty()) return;

        BlockPos pos = closest.get();
        //check for side with air, prefering sides facing the player
        Vec3i playerPos = player.blockPosition().relative(Direction.UP);
        Direction freeDirection = Direction.stream()
                .filter(p -> world.getBlockState(pos.relative(p)).isAir())
                .min(Comparator.comparingDouble(p -> playerPos.distSqr(pos.relative(p))))
                .orElse(null);

        if (freeDirection == null) return;

        //interact with block
        updatedBlocks.put(pos, System.currentTimeMillis());
        lastInteractedBlock = pos;
        lastClickedDirection = freeDirection;
        autoUpdateTask = new TaskManager().then(new MaxTimeTask(
                new WaitForInventoryTask(Pattern.compile(".*"), containerInfo ->
                        updateInventory(containerInfo.getItems()))
                        .then(() -> lastInteractedBlock = null)
                        .then(() -> lastClickedDirection = null)
                        .then(at.haha007.edenclient.utils.ContainerInfo::clear)
                        .then(new SyncTask(PlayerUtils.getPlayer()::closeContainer)
                                .then(() -> autoUpdateTask = null)),
                100, () -> {
            autoUpdateTask = null;
            lastInteractedBlock = null;
            lastClickedDirection = null;
            at.haha007.edenclient.utils.ContainerInfo.clear();
            LogUtils.getLogger().info("failed to update container at {}", pos);
        }));
        autoUpdateTask.start();
        LogUtils.getLogger().info("fetching container at {}", pos);
        player.connection.send(new ServerboundUseItemOnPacket(InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(pos), freeDirection, pos, false), 1));
    }

    private void onInventoryChange(Inventory playerInventory) {
        AbstractContainerMenu sh = playerInventory.player.containerMenu;
        if (!(sh instanceof ShulkerBoxMenu) && !(sh instanceof ChestMenu)) {
            lastInteractedBlock = null;
            return;
        }

        List<ItemStack> items = sh.getItems();
        items = items.subList(0, items.size() - 36);
        if (items.isEmpty()) lastInteractedBlock = null;
        else updateInventory(items);
    }

    private void updateInventory(List<ItemStack> itemStacks) {
        if (lastInteractedBlock == null) return;
        //smallest only chests and shulkerboxes!
        if (itemStacks.size() < 27) return;
        @SuppressWarnings("resource")
        Level level = PlayerUtils.getPlayer().level();
        Registry<Block> registry = level.registryAccess().lookupOrThrow(BlockTags.SHULKER_BOXES.registry());
        Map<Item, List<ItemStack>> items = itemStacks.stream().
                flatMap(stack -> registry.containsKey(BuiltInRegistries.BLOCK.getKey(Block.byItem(stack.getItem()))) ?
                        mapShulkerBox(stack) : Stream.of(stack)).collect(Collectors.groupingBy(ItemStack::getItem));
        items.remove(Items.AIR);

        Map<Item, Integer> counts = new HashMap<>();
        items.forEach((item, stacks) -> counts.put(item, stacks.stream().mapToInt(ItemStack::getCount).sum()));

        BlockEntity blockEntity = level.getBlockEntity(lastInteractedBlock);
        if (!(blockEntity instanceof BaseContainerBlockEntity)) {
            return;
        }

        List<Item> itemsList = new ArrayList<>(counts.keySet());
        itemsList.sort(Comparator.comparingInt(i -> -counts.get(i)));

        putChestInfo(lastInteractedBlock, itemsList, lastClickedDirection);

        BlockState blockState = level.getBlockState(lastInteractedBlock);
        if (!(blockEntity instanceof ChestBlockEntity)) {
            return;
        }
        ChestType chestType = blockState.getValue(BlockStateProperties.CHEST_TYPE);
        if (chestType == ChestType.SINGLE) {
            return;
        }
        Direction connectedDirection = ChestBlock.getConnectedDirection(blockState);
        BlockPos otherPos = lastInteractedBlock.relative(connectedDirection);
        putChestInfo(otherPos, itemsList, null);
    }

    private void putChestInfo(Vec3i position, List<Item> items, @Nullable Direction face) {
        ChunkPos chunkPos = new ChunkPos(new BlockPos(lastInteractedBlock));
        ChestMap map = chunkMap.computeIfAbsent(chunkPos, cp -> new ChestMap());
        if (items.isEmpty()) {
            map.remove(position);
            return;
        }

        ChestInfo chestInfo = map.computeIfAbsent(position, pos -> new ChestInfo());
        chestInfo.items.clear();
        chestInfo.items.addAll(items);
        if (face != null) {
            chestInfo.face = face;
        }
    }

    private Stream<ItemStack> mapShulkerBox(ItemStack stack) {
        ItemContainerContents containerContents = stack.getComponents().get(DataComponents.CONTAINER);
        if (containerContents == null || containerContents.copyOne() == ItemStack.EMPTY) return Stream.of(stack);
        return containerContents.stream();
    }

    private InteractionResult attackBlock(LocalPlayer player, BlockPos blockPos, Direction direction) {
        lastInteractedBlock = null;
        ClientLevel world = Minecraft.getInstance().level;
        BlockEntity be = world.getBlockEntity(blockPos);
        if (!(be instanceof Container)) return InteractionResult.PASS;
        LevelChunk chunk = world.getChunkAt(blockPos);
        EdenClient.getMod(Scheduler.class).scheduleSyncDelayed(() -> updateChunk(chunk), 1);
        return InteractionResult.PASS;
    }

    private InteractionResult interactBlock(LocalPlayer player, ClientLevel world, InteractionHand
            hand, BlockHitResult blockHitResult) {
        //remove all positions in same chunk without block entity
        ChunkPos cp = new ChunkPos(blockHitResult.getBlockPos());
        ChestMap chestMap = chunkMap.getOrDefault(cp, new ChestMap());
        Set<Vec3i> chests = chestMap.keySet();
        chests.removeIf(Predicate.not(e -> world.getBlockEntity(new BlockPos(e)) instanceof Container));
        BlockEntity be = world.getBlockEntity(blockHitResult.getBlockPos());
        if (be instanceof Container) {
            lastInteractedBlock = blockHitResult.getBlockPos();
            lastClickedDirection = blockHitResult.getDirection();
        } else {
            lastInteractedBlock = null;
        }
        return InteractionResult.PASS;
    }

    private void updateChunk(LevelChunk chunk) {
        if (chunk == null) return;
        if (chunk.isEmpty()) return;
        Map<BlockPos, BlockEntity> be = Map.copyOf(chunk.getBlockEntities());
        if (be.isEmpty()) return;
        ChestMap map = chunkMap.get(chunk.getPos());
        if (map == null) return;
        map.keySet().removeIf(Predicate.not(e -> be.containsKey(new BlockPos(e))));
    }


    public ChestMap getContainerInfo(ChunkPos chunkPos) {
        return chunkMap.containsKey(chunkPos) ? chunkMap.get(chunkPos) : new ChestMap();
    }

    public static class ChestMap extends HashMap<Vec3i, ChestInfo> {
    }

    public static class ChestInfo {
        private ItemList items = new ItemList();
        private Direction face = Direction.NORTH;

        public List<Item> items() {
            return items;
        }

        public Direction face() {
            return face;
        }
    }

    private static class ChestInfoLoader implements ConfigLoader<CompoundTag, ChestInfo> {
        @Override
        @NotNull
        public CompoundTag save(@NotNull ChestInfo chestInfo) {
            CompoundTag compound = new CompoundTag();
            compound.put("items", PerWorldConfig.get().toNbt(chestInfo.items));
            compound.putString("direction", chestInfo.face.getName());
            return compound;
        }

        @Override
        @NotNull
        public ChestInfo load(@NotNull CompoundTag tag) {
            ChestInfo chestInfo = new ChestInfo();
            Tag itemsCompound = tag.get("items");
            chestInfo.items = itemsCompound == null ? new ItemList() : PerWorldConfig.get().toObject(itemsCompound, ItemList.class);
            chestInfo.face = Direction.byName(tag.getString("direction").orElseThrow());
            return chestInfo;
        }

        @Override
        @NotNull
        public CompoundTag parse(@NotNull String s) {
            return new CompoundTag();
        }
    }

    private static class ChestMapLoader implements ConfigLoader<ListTag, ChestMap> {

        @NotNull
        public ListTag save(@NotNull ChestMap map) {
            ListTag tag = new ListTag();
            map.forEach((k, v) -> {
                CompoundTag c = new CompoundTag();
                c.put("pos", PerWorldConfig.get().toNbt(k));
                c.put("info", PerWorldConfig.get().toNbt(v));
                tag.add(c);
            });
            return tag;
        }

        @NotNull
        public ChestMap load(@NotNull ListTag tag) {
            ChestMap map = new ChestMap();
            tag.forEach(e -> {
                CompoundTag c = (CompoundTag) e;
                Vec3i bp = PerWorldConfig.get().toObject(c.get("pos"), Vec3i.class);
                ChestInfo itemList = PerWorldConfig.get().toObject(c.get("info"), ChestInfo.class);
                map.put(bp, itemList);
            });
            return map;
        }

        @NotNull
        public ListTag parse(@NotNull String s) {
            return new ListTag();
        }
    }

    public static class ChunkChestMap extends HashMap<ChunkPos, ChestMap> {
    }

    private static class ContainerConfigLoader implements ConfigLoader<ListTag, ChunkChestMap> {
        @NotNull
        public ListTag save(@NotNull ChunkChestMap map) {
            ListTag list = new ListTag();
            map.forEach((k, v) -> {
                CompoundTag c = new CompoundTag();
                c.put("pos", PerWorldConfig.get().toNbt(k));
                c.put("map", PerWorldConfig.get().toNbt(v));
                list.add(c);
            });
            return list;
        }

        @NotNull
        public ChunkChestMap load(@NotNull ListTag tag) {
            ChunkChestMap map = new ChunkChestMap();
            tag.forEach(e -> {
                CompoundTag c = (CompoundTag) e;
                ChunkPos bp = PerWorldConfig.get().toObject(c.get("pos"), ChunkPos.class);
                ChestMap itemList = PerWorldConfig.get().toObject(c.get("map"), ChestMap.class);
                map.put(bp, itemList);
            });
            return map;
        }

        @NotNull
        public ListTag parse(@NotNull String s) {
            return new ListTag();
        }
    }

    private enum AutoMode {
        EMPTY,
        ALL,
        OLD,
        OLD_EMPTY
    }

    private void removeOldEntriesFromUpdatedBlocks(int seconds) {
        updatedBlocks.entrySet().stream()
                .filter(e -> e.getValue() < System.currentTimeMillis() - seconds * 1000L)
                .toList()
                .forEach(e -> updatedBlocks.remove(e.getKey()));
    }

    private boolean matchesEmpty(BlockPos pos, ClientLevel level) {
        if (updatedBlocks.containsKey(pos)) return false;
        LevelChunk levelChunk = level.getChunkAt(pos);
        if (levelChunk.isEmpty()) return false;
        ChunkPos chunkPos = levelChunk.getPos();
        ChestMap chestMap = chunkMap.get(chunkPos);
        if (chestMap == null) return true;
        ChestInfo chestInfo = chestMap.get(pos);
        if (chestInfo == null) return true;
        return chestInfo.items.isEmpty();
    }

    private boolean matchesOld(BlockPos pos, int seconds) {
        long lastUpdated = updatedBlocks.getOrDefault(pos, 0L);
        return lastUpdated < System.currentTimeMillis() - seconds * 1000L;
    }
}
