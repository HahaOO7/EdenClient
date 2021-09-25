package at.haha007.edenclient.mods.datafetcher;

import at.haha007.edenclient.callbacks.*;
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
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ShulkerBoxScreenHandler;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.chunk.WorldChunk;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class ContainerInfo {

    private final Map<ChunkPos, Map<BlockPos, List<Item>>> chunkMap;
    private BlockPos lastInteractedBlock = null;


    ContainerInfo() {
        chunkMap = new HashMap<>();
        PlayerTickCallback.EVENT.register(this::tick);
        PlayerAttackBlockCallback.EVENT.register(this::attackBlock);
        PlayerInteractBlockCallback.EVENT.register(this::interactBlock);
        InventoryOpenCallback.EVENT.register(this::onOpenInventory);
        PlayerInvChangeCallback.EVENT.register(this::onInventoryChange);
    }

    private void onInventoryChange(PlayerInventory playerInventory) {
        var sh = playerInventory.player.currentScreenHandler;
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
        ChunkPos cp = new ChunkPos(lastInteractedBlock);

        Map<Item, List<ItemStack>> items = itemStacks.stream().collect(Collectors.groupingBy(ItemStack::getItem));
        items.remove(Items.AIR);

        Map<Item, Integer> counts = new HashMap<>();
        items.forEach((item, stacks) -> counts.put(item, stacks.stream().mapToInt(ItemStack::getCount).sum()));

        List<Item> list = new ArrayList<>(counts.keySet());
        list.sort(Comparator.comparingInt(i -> -counts.get(i)));

        Map<BlockPos, List<Item>> map = chunkMap.computeIfAbsent(cp, chunkPos -> new HashMap<>());
        if (list.isEmpty())
            map.remove(lastInteractedBlock);
        else
            map.put(lastInteractedBlock, list);
    }

    private ActionResult attackBlock(ClientPlayerEntity player, BlockPos blockPos, Direction direction) {
        lastInteractedBlock = null;
        return ActionResult.PASS;
    }

    private ActionResult interactBlock(ClientPlayerEntity player, ClientWorld world, Hand hand, BlockHitResult blockHitResult) {
        BlockEntity be = world.getBlockEntity(blockHitResult.getBlockPos());
        boolean isLeftChest = false;
        if (be instanceof ChestBlockEntity chest) {
            BlockState blockState = world.getBlockState(blockHitResult.getBlockPos());
            isLeftChest = blockState.get(ChestBlock.CHEST_TYPE) == ChestType.LEFT;
        }
        if (be instanceof Inventory && !isLeftChest) {
            lastInteractedBlock = blockHitResult.getBlockPos();
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
        Map<BlockPos, List<Item>> map = chunkMap.get(chunk.getPos());
        if (map == null) return;
        map.keySet().removeIf(Predicate.not(be::containsKey));
    }


    public Map<BlockPos, List<Item>> getContainerInfo(ChunkPos chunkPos) {
        return chunkMap.containsKey(chunkPos) ? chunkMap.get(chunkPos) : new HashMap<>();
    }
}
