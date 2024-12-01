package at.haha007.edenclient.mods.datafetcher;

import at.haha007.edenclient.callbacks.*;
import at.haha007.edenclient.utils.PlayerUtils;
import at.haha007.edenclient.utils.config.ConfigSubscriber;
import at.haha007.edenclient.utils.config.PerWorldConfig;
import at.haha007.edenclient.utils.config.loaders.ConfigLoader;
import at.haha007.edenclient.utils.config.wrappers.ItemList;
import net.minecraft.client.multiplayer.ClientChunkCache;
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
import net.minecraft.tags.BlockTags;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ShulkerBoxMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ContainerInfo {
    @ConfigSubscriber()
    private final ChunkChestMap chunkMap;
    private Vec3i lastInteractedBlock = null;
    private Direction lastClickedDirection = null;


    ContainerInfo() {
        chunkMap = new ChunkChestMap();

        PlayerTickCallback.EVENT.register(this::tick, getClass());
        PlayerAttackBlockCallback.EVENT.register(this::attackBlock, getClass());
        PlayerInteractBlockCallback.EVENT.register(this::interactBlock, getClass());
        ContainerCloseCallback.EVENT.register(this::onCloseInventory, getClass());
        PlayerInvChangeCallback.EVENT.register(this::onInventoryChange, getClass());

        PerWorldConfig.get().register(new ContainerConfigLoader(), ChunkChestMap.class);
        PerWorldConfig.get().register(new ChestMapLoader(), ChestMap.class);
        PerWorldConfig.get().register(new ChestInfoLoader(), ChestInfo.class);
        PerWorldConfig.get().register(this, "ContainerInfo");

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
        else onCloseInventory(items);
    }

    private void onCloseInventory(List<ItemStack> itemStacks) {
        if (lastInteractedBlock == null) return;
        //smallest only chests and shulkerboxes!
        if (itemStacks.size() < 27) return;
        ChunkPos cp = new ChunkPos(new BlockPos(lastInteractedBlock));
        @SuppressWarnings("resource")
        Level level = PlayerUtils.getPlayer().level();
        Registry<Block> registry = level.registryAccess().lookupOrThrow(BlockTags.SHULKER_BOXES.registry());
        Map<Item, List<ItemStack>> items = itemStacks.stream().
                flatMap(stack -> registry.containsKey(BuiltInRegistries.BLOCK.getKey(Block.byItem(stack.getItem()))) ?
                        mapShulkerBox(stack) : Stream.of(stack)).collect(Collectors.groupingBy(ItemStack::getItem));
        items.remove(Items.AIR);

        Map<Item, Integer> counts = new HashMap<>();
        items.forEach((item, stacks) -> counts.put(item, stacks.stream().mapToInt(ItemStack::getCount).sum()));

        ChestInfo chestInfo = new ChestInfo();
        chestInfo.items.addAll(counts.keySet());
        chestInfo.items.sort(Comparator.comparingInt(i -> -counts.get(i)));
        chestInfo.face = lastClickedDirection == null ? Direction.UP : lastClickedDirection;

        ChestMap map = chunkMap.computeIfAbsent(cp, chunkPos -> new ChestMap());
        if (chestInfo.items.isEmpty())
            map.remove(lastInteractedBlock);
        else
            map.put(lastInteractedBlock, chestInfo);
    }

    private Stream<ItemStack> mapShulkerBox(ItemStack stack) {
        ItemContainerContents containerContents = stack.getComponents().get(DataComponents.CONTAINER);
        if(containerContents == null || containerContents.copyOne() == ItemStack.EMPTY) return Stream.of(stack);
        return containerContents.stream();
    }

    private InteractionResult attackBlock(LocalPlayer player, BlockPos blockPos, Direction direction) {
        lastInteractedBlock = null;
        return InteractionResult.PASS;
    }

    private InteractionResult interactBlock(LocalPlayer player, ClientLevel world, InteractionHand hand, BlockHitResult blockHitResult) {
        BlockEntity be = world.getBlockEntity(blockHitResult.getBlockPos());
        if (be instanceof Container) {
            lastInteractedBlock = blockHitResult.getBlockPos();
            lastClickedDirection = blockHitResult.getDirection();
        } else {
            lastInteractedBlock = null;
        }
        return InteractionResult.PASS;
    }

    private void tick(LocalPlayer player) {
        ChunkPos center = player.chunkPosition();
        ClientChunkCache cm = player.clientLevel.getChunkSource();
        ChunkPos.rangeClosed(center, 2).
                map(cp -> cm.getChunk(cp.x, cp.z, false)).
                filter(Objects::nonNull).
                forEach(this::updateChunk);
    }

    private void updateChunk(LevelChunk chunk) {
        Map<BlockPos, BlockEntity> be = chunk.getBlockEntities();
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
            chestInfo.items = PerWorldConfig.get().toObject(itemsCompound, ItemList.class);
            chestInfo.face = Direction.byName(tag.getString("direction"));
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

    private static class ChunkChestMap extends HashMap<ChunkPos, ChestMap> {
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
}
