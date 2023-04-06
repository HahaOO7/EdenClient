package at.haha007.edenclient.mods.datafetcher;

import at.haha007.edenclient.callbacks.*;
import at.haha007.edenclient.utils.PlayerUtils;
import at.haha007.edenclient.utils.config.ConfigSubscriber;
import at.haha007.edenclient.utils.config.PerWorldConfig;
import at.haha007.edenclient.utils.config.loaders.ConfigLoader;
import at.haha007.edenclient.utils.config.wrappers.ItemList;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.enums.ChestType;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientChunkManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ShulkerBoxScreenHandler;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.chunk.WorldChunk;

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

    private void onInventoryChange(PlayerInventory playerInventory) {
        ScreenHandler sh = playerInventory.player.currentScreenHandler;
        if (!(sh instanceof ShulkerBoxScreenHandler) && !(sh instanceof GenericContainerScreenHandler)) {
            lastInteractedBlock = null;
            return;
        }

        List<ItemStack> items = sh.getStacks();
        items = items.subList(0, items.size() - 36);
        if (items.isEmpty()) lastInteractedBlock = null;
        else onOpenInventory(items);
    }

    private void onOpenInventory(List<ItemStack> itemStacks) {
        if (lastInteractedBlock == null) return;
        //smallest only chests and shulkerboxes!
        if (itemStacks.size() < 27) return;
        ChunkPos cp = new ChunkPos(new BlockPos(lastInteractedBlock));
        Registry<Block> registry = PlayerUtils.getPlayer().world.getRegistryManager().get(BlockTags.SHULKER_BOXES.registry());
        Map<Item, List<ItemStack>> items = itemStacks.stream().
                flatMap(stack -> registry.containsId(Registries.BLOCK.getId(Block.getBlockFromItem(stack.getItem()))) ?
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
        NbtCompound tag = stack.getOrCreateNbt();
        NbtList list = tag.getCompound("BlockEntityTag").getList("Items", NbtElement.COMPOUND_TYPE);
        if (list.isEmpty())
            return Stream.of(stack);
        return list.stream().map(nbt -> (NbtCompound) nbt).map(this::getStackFromCompound);
    }

    private ItemStack getStackFromCompound(NbtCompound tag) {
        Item item = Registries.ITEM.get(new Identifier(tag.getString("id")));
        ItemStack stack = item.getDefaultStack();
        stack.setCount(tag.getByte("Count"));
        return stack;
    }

    private ActionResult attackBlock(ClientPlayerEntity player, BlockPos blockPos, Direction direction) {
        lastInteractedBlock = null;
        return ActionResult.PASS;
    }

    private ActionResult interactBlock(ClientPlayerEntity player, ClientWorld world, Hand hand, BlockHitResult blockHitResult) {
        BlockEntity be = world.getBlockEntity(blockHitResult.getBlockPos());
        boolean isLeftChest = false;
        if (be instanceof ChestBlockEntity) {
            BlockState blockState = world.getBlockState(blockHitResult.getBlockPos());
            isLeftChest = blockState.get(ChestBlock.CHEST_TYPE) == ChestType.LEFT;
        }
        if (be instanceof Inventory && !isLeftChest) {
            lastInteractedBlock = blockHitResult.getBlockPos();
            lastClickedDirection = blockHitResult.getSide();
        } else {
            lastInteractedBlock = null;
        }
        return ActionResult.PASS;
    }

    private void tick(ClientPlayerEntity player) {
        ChunkPos center = player.getChunkPos();
        ClientChunkManager cm = player.clientWorld.getChunkManager();
        ChunkPos.stream(center, 2).
                map(cp -> cm.getWorldChunk(cp.x, cp.z, false)).
                filter(Objects::nonNull).
                forEach(this::updateChunk);
    }

    private void updateChunk(WorldChunk chunk) {
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

    private static class ChestInfoLoader implements ConfigLoader<NbtCompound, ChestInfo> {
        @Override
        public NbtCompound save(Object value) {
            ChestInfo chestInfo = cast(value);
            NbtCompound compound = new NbtCompound();
            compound.put("items", PerWorldConfig.get().toNbt(chestInfo.items));
            compound.putString("direction", chestInfo.face.getName());
            return compound;
        }

        @Override
        public ChestInfo load(NbtCompound tag) {
            ChestInfo chestInfo = new ChestInfo();
            NbtCompound itemsCompound = tag.getCompound("items");
            chestInfo.items = PerWorldConfig.get().toObject(itemsCompound, ItemList.class);
            chestInfo.face = Direction.byName(tag.getString("direction"));
            return chestInfo;
        }

        @Override
        public NbtCompound parse(String s) {
            return new NbtCompound();
        }
    }

    private static class ChestMapLoader implements ConfigLoader<NbtList, ChestMap> {
        public NbtList save(Object value) {
            NbtList tag = new NbtList();
            ChestMap map = cast(value);
            map.forEach((k, v) -> {
                NbtCompound c = new NbtCompound();
                c.put("pos", PerWorldConfig.get().toNbt(k));
                c.put("info", PerWorldConfig.get().toNbt(v));
                tag.add(c);
            });
            return tag;
        }

        public ChestMap load(NbtList tag) {
            ChestMap map = new ChestMap();
            tag.forEach(e -> {
                NbtCompound c = (NbtCompound) e;
                Vec3i bp = PerWorldConfig.get().toObject(c.get("pos"), Vec3i.class);
                ChestInfo itemList = PerWorldConfig.get().toObject(c.get("info"), ChestInfo.class);
                map.put(bp, itemList);
            });
            return map;
        }

        public NbtList parse(String s) {
            return new NbtList();
        }
    }

    private static class ChunkChestMap extends HashMap<ChunkPos, ChestMap> {
    }

    private static class ContainerConfigLoader implements ConfigLoader<NbtList, ChunkChestMap> {
        public NbtList save(Object value) {
            ChunkChestMap map = cast(value);
            NbtList list = new NbtList();
            map.forEach((k, v) -> {
                NbtCompound c = new NbtCompound();
                c.put("pos", PerWorldConfig.get().toNbt(k));
                c.put("map", PerWorldConfig.get().toNbt(v));
                list.add(c);
            });
            return list;
        }

        public ChunkChestMap load(NbtList tag) {
            ChunkChestMap map = new ChunkChestMap();
            tag.forEach(e -> {
                NbtCompound c = (NbtCompound) e;
                ChunkPos bp = PerWorldConfig.get().toObject(c.get("pos"), ChunkPos.class);
                ChestMap itemList = PerWorldConfig.get().toObject(c.get("map"), ChestMap.class);
                map.put(bp, itemList);
            });
            return map;
        }

        public NbtList parse(String s) {
            return new NbtList();
        }
    }
}
