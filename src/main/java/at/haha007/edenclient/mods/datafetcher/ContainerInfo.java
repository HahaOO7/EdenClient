package at.haha007.edenclient.mods.datafetcher;

import at.haha007.edenclient.callbacks.*;
import at.haha007.edenclient.utils.PlayerUtils;
import at.haha007.edenclient.utils.config.ConfigSubscriber;
import at.haha007.edenclient.utils.config.PerWorldConfig;
import at.haha007.edenclient.utils.config.loaders.ConfigLoader;
import at.haha007.edenclient.utils.config.wrappers.ItemList;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.client.multiplayer.ClientChunkCache;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
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
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.BlockHitResult;

public class ContainerInfo {
    @ConfigSubscriber()
    private final ChunkChestMap chunkMap;
    private Vec3i lastInteractedBlock = null;
    private Direction lastClickedDirection = null;


    ContainerInfo() {
        chunkMap = new ChunkChestMap();
        PlayerTickCallback.EVENT.register(this::tick);
        PlayerAttackBlockCallback.EVENT.register(this::attackBlock);
        PlayerInteractBlockCallback.EVENT.register(this::interactBlock);
        InventoryOpenCallback.EVENT.register(this::onOpenInventory);
        PlayerInvChangeCallback.EVENT.register(this::onInventoryChange);
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
        else onOpenInventory(items);
    }

    private void onOpenInventory(List<ItemStack> itemStacks) {
        if (lastInteractedBlock == null) return;
        //smallest only chests and shulkerboxes!
        if (itemStacks.size() < 27) return;
        ChunkPos cp = new ChunkPos(new BlockPos(lastInteractedBlock));
        Registry<Block> registry = PlayerUtils.getPlayer().level().registryAccess().registryOrThrow(BlockTags.SHULKER_BOXES.registry());
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

    private Stream<? extends ItemStack> mapShulkerBox(ItemStack stack) {
        CompoundTag tag = stack.getOrCreateTag();
        ListTag list = tag.getCompound("BlockEntityTag").getList("Items", Tag.TAG_COMPOUND);
        if (list.isEmpty())
            return Stream.of(stack);
        return list.stream().map(nbt -> (CompoundTag) nbt).map(this::getStackFromCompound);
    }

    private ItemStack getStackFromCompound(CompoundTag tag) {
        Item item = BuiltInRegistries.ITEM.get(new ResourceLocation(tag.getString("id")));
        ItemStack stack = item.getDefaultInstance();
        stack.setCount(tag.getByte("Count"));
        return stack;
    }

    private InteractionResult attackBlock(LocalPlayer player, BlockPos blockPos, Direction direction) {
        lastInteractedBlock = null;
        return InteractionResult.PASS;
    }

    private InteractionResult interactBlock(LocalPlayer player, ClientLevel world, InteractionHand hand, BlockHitResult blockHitResult) {
        BlockEntity be = world.getBlockEntity(blockHitResult.getBlockPos());
        boolean isLeftChest = false;
        if (be instanceof ChestBlockEntity) {
            BlockState blockState = world.getBlockState(blockHitResult.getBlockPos());
            isLeftChest = blockState.getValue(ChestBlock.TYPE) == ChestType.LEFT;
        }
        if (be instanceof Container && !isLeftChest) {
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

    private static class ChestMap extends HashMap<Vec3i, ChestInfo> {
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
        public CompoundTag save(Object value) {
            ChestInfo chestInfo = cast(value);
            CompoundTag compound = new CompoundTag();
            compound.put("items", PerWorldConfig.get().toNbt(chestInfo.items));
            compound.putString("direction", chestInfo.face.getName());
            return compound;
        }

        @Override
        public ChestInfo load(CompoundTag tag) {
            ChestInfo chestInfo = new ChestInfo();
            Tag itemsCompound = tag.get("items");
            chestInfo.items = PerWorldConfig.get().toObject(itemsCompound, ItemList.class);
            chestInfo.face = Direction.byName(tag.getString("direction"));
            return chestInfo;
        }

        @Override
        public CompoundTag parse(String s) {
            return new CompoundTag();
        }
    }

    private static class ChestMapLoader implements ConfigLoader<ListTag, ChestMap> {
        public ListTag save(Object value) {
            ListTag tag = new ListTag();
            ChestMap map = cast(value);
            map.forEach((k, v) -> {
                CompoundTag c = new CompoundTag();
                c.put("pos", PerWorldConfig.get().toNbt(k));
                c.put("info", PerWorldConfig.get().toNbt(v));
                tag.add(c);
            });
            return tag;
        }

        public ChestMap load(ListTag tag) {
            ChestMap map = new ChestMap();
            tag.forEach(e -> {
                CompoundTag c = (CompoundTag) e;
                Vec3i bp = PerWorldConfig.get().toObject(c.get("pos"), Vec3i.class);
                ChestInfo itemList = PerWorldConfig.get().toObject(c.get("info"), ChestInfo.class);
                map.put(bp, itemList);
            });
            return map;
        }

        public ListTag parse(String s) {
            return new ListTag();
        }
    }

    private static class ChunkChestMap extends HashMap<ChunkPos, ChestMap> {
    }

    private static class ContainerConfigLoader implements ConfigLoader<ListTag, ChunkChestMap> {
        public ListTag save(Object value) {
            ChunkChestMap map = cast(value);
            ListTag list = new ListTag();
            map.forEach((k, v) -> {
                CompoundTag c = new CompoundTag();
                c.put("pos", PerWorldConfig.get().toNbt(k));
                c.put("map", PerWorldConfig.get().toNbt(v));
                list.add(c);
            });
            return list;
        }

        public ChunkChestMap load(ListTag tag) {
            ChunkChestMap map = new ChunkChestMap();
            tag.forEach(e -> {
                CompoundTag c = (CompoundTag) e;
                ChunkPos bp = PerWorldConfig.get().toObject(c.get("pos"), ChunkPos.class);
                ChestMap itemList = PerWorldConfig.get().toObject(c.get("map"), ChestMap.class);
                map.put(bp, itemList);
            });
            return map;
        }

        public ListTag parse(String s) {
            return new ListTag();
        }
    }
}
