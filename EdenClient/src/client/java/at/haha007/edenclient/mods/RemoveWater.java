package at.haha007.edenclient.mods;

import at.haha007.edenclient.annotations.Mod;
import at.haha007.edenclient.callbacks.ConfigLoadedCallback;
import at.haha007.edenclient.callbacks.JoinWorldCallback;
import at.haha007.edenclient.callbacks.PlayerTickCallback;
import at.haha007.edenclient.utils.PlayerUtils;
import at.haha007.edenclient.utils.area.BlockArea;
import at.haha007.edenclient.utils.area.SavableBlockArea;
import at.haha007.edenclient.utils.config.ConfigSubscriber;
import at.haha007.edenclient.utils.config.PerWorldConfig;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

import java.util.*;

import static at.haha007.edenclient.command.CommandManager.*;
import static at.haha007.edenclient.utils.PlayerUtils.sendModMessage;

@Mod
public class RemoveWater {
    @ConfigSubscriber("false")
    private boolean enabled = false;
    @ConfigSubscriber("4.5")
    private double range = 4.5;
    @ConfigSubscriber("1")
    private int rate = 1;
    @ConfigSubscriber("false")
    private boolean useArea = false;
    @ConfigSubscriber("0,0,0,0,0,0")
    private SavableBlockArea area;

    private static final int PLACEMENT_TIMEOUT_TICKS = 10;
    private static final BlockState PLACEMENT_TEST_STATE = Blocks.STONE.defaultBlockState();

    private final Map<BlockPos, Integer> pendingPlacements = new HashMap<>();
    private BlockPos breakTarget;
    private Direction breakDirection;
    private boolean breakStarted;
    private float breakProgress;

    public RemoveWater() {
        PlayerTickCallback.EVENT.register(this::tick, getClass());
        JoinWorldCallback.EVENT.register(this::onJoinWorld, getClass());
        ConfigLoadedCallback.EVENT.register(this::onConfigLoaded, getClass());
        PerWorldConfig.get().register(this, "removeWater");
        registerCommand();
    }

    private void tick(LocalPlayer player) {
        if (!enabled) {
            abortBreak(player);
            pendingPlacements.clear();
            return;
        }
        if (PlayerUtils.shouldPlayLegit()) {
            abortBreak(player);
            return;
        }

        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        if (useArea && area == null) {
            sendModMessage("RemoveWater area is not defined. Use \"/eremovewater area\" first.");
            enabled = false;
            return;
        }

        pendingPlacements.replaceAll((_, ticks) -> ticks - 1);
        pendingPlacements.entrySet().removeIf(entry -> entry.getValue() <= 0 || !isWater(level, entry.getKey()));

        if (breakTarget != null && !isValidBreakTarget(player, level, breakTarget)) {
            abortBreak(player);
        }
        if (breakTarget == null) {
            breakTarget = findBreakTarget(player, level);
            if (breakTarget != null) {
                breakStarted = false;
                breakDirection = null;
                breakProgress = 0.0F;
            }
        }
        if (breakTarget != null) {
            continueBreaking(player, level);
            return;
        }

        int placementSlot = findPlacementSlot(player, level);
        if (placementSlot < 0) {
            return;
        }
        ItemStack placementStack = player.getInventory().getItem(placementSlot);

        List<BlockPos> placeTargets = findPlacementTargets(player, level, rate, pendingPlacements.keySet(), placementStack);
        for (BlockPos placeTarget : placeTargets) {
            if (!selectPlacementBlock(player, level)) {
                break;
            }
            placeBlock(placeTarget, player);
            pendingPlacements.put(placeTarget, PLACEMENT_TIMEOUT_TICKS);
        }
    }

    private BlockPos findBreakTarget(LocalPlayer player, ClientLevel level) {
        BlockPos center = player.blockPosition();
        int radius = (int) Math.ceil(range) + 1;
        Vec3 eye = player.getEyePosition();

        return BlockPos.betweenClosedStream(
                        center.offset(-radius, -radius, -radius),
                        center.offset(radius, radius, radius))
                .map(BlockPos::immutable)
                .filter(pos -> Vec3.atCenterOf(pos).closerThan(eye, range))
                .filter(pos -> !useArea || (area != null && area.contains(pos)))
                .filter(pos -> isWater(level, pos))
                .filter(pos -> !level.getBlockState(pos).canBeReplaced())
                .min(Comparator.comparingDouble(pos -> pos.distSqr(center)))
                .orElse(null);
    }

    private List<BlockPos> findPlacementTargets(LocalPlayer player,
                                                ClientLevel level,
                                                int limit,
                                                Set<BlockPos> excluded,
                                                ItemStack placementStack) {
        if (limit <= 0) {
            return List.of();
        }
        BlockPos center = player.blockPosition();
        int radius = (int) Math.ceil(range) + 1;
        Vec3 eye = player.getEyePosition();

        List<BlockPos> candidates = BlockPos.betweenClosedStream(
                        center.offset(-radius, -radius, -radius),
                        center.offset(radius, radius, radius))
                .map(BlockPos::immutable)
                .filter(pos -> Vec3.atCenterOf(pos).closerThan(eye, range))
                .filter(pos -> !excluded.contains(pos))
                .filter(pos -> !useArea || (area != null && area.contains(pos)))
                .filter(pos -> isWater(level, pos))
                .filter(pos -> canPlaceAt(player, level, pos, placementStack))
                .sorted(Comparator.comparingDouble(pos -> pos.distSqr(center)))
                .toList();

        List<BlockPos> targets = new ArrayList<>(Math.min(limit, candidates.size()));
        for (BlockPos pos : candidates) {
            if (level.isUnobstructed(PLACEMENT_TEST_STATE, pos, CollisionContext.placementContext(player))) {
                targets.add(pos);
                if (targets.size() >= limit) {
                    break;
                }
            }
        }
        return targets;
    }

    private boolean canPlaceAt(LocalPlayer player, ClientLevel level, BlockPos pos, ItemStack stack) {
        return new BlockPlaceContext(level, player, InteractionHand.MAIN_HAND, stack, placementHit(pos)).canPlace();
    }

    private static BlockHitResult placementHit(BlockPos pos) {
        return new BlockHitResult(Vec3.atBottomCenterOf(pos.above()), Direction.UP, pos, false);
    }

    private boolean isValidBreakTarget(LocalPlayer player, ClientLevel level, BlockPos pos) {
        if (!isWater(level, pos)) {
            return false;
        }
        if (level.getBlockState(pos).canBeReplaced()) {
            return false;
        }
        if (useArea && (area == null || !area.contains(pos))) {
            return false;
        }
        return Vec3.atCenterOf(pos).closerThan(player.getEyePosition(), range);
    }

    private void continueBreaking(LocalPlayer player, ClientLevel level) {
        selectBestTool(breakTarget, player, level);
        BlockState state = level.getBlockState(breakTarget);
        Direction direction = PlayerUtils.getHitDirectionForBlock(player, breakTarget);

        if (!breakStarted) {
            player.connection.send(new ServerboundPlayerActionPacket(
                    ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK,
                    breakTarget,
                    direction));
            breakStarted = true;
            breakDirection = direction;
            breakProgress = state.getDestroyProgress(player, level, breakTarget);
        } else {
            if (direction != breakDirection) {
                breakDirection = direction;
                player.connection.send(new ServerboundPlayerActionPacket(
                        ServerboundPlayerActionPacket.Action.CHANGE_DESTROY_DIRECTION,
                        breakTarget,
                        direction));
            }
            breakProgress += state.getDestroyProgress(player, level, breakTarget);
        }

        if (breakProgress < 1.0F) {
            return;
        }
        player.connection.send(new ServerboundPlayerActionPacket(
                ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK,
                breakTarget,
                direction));
        breakTarget = null;
        breakStarted = false;
        breakDirection = null;
        breakProgress = 0.0F;
    }

    private void abortBreak(LocalPlayer player) {
        if (breakStarted && breakTarget != null) {
            Direction direction = breakDirection == null ? Direction.UP : breakDirection;
            player.connection.send(new ServerboundPlayerActionPacket(
                    ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK,
                    breakTarget,
                    direction));
        }
        breakTarget = null;
        breakStarted = false;
        breakDirection = null;
        breakProgress = 0.0F;
    }

    private boolean isWater(ClientLevel level, BlockPos pos) {
        FluidState fluidState = level.getFluidState(pos);
        return fluidState.is(Fluids.WATER);
    }

    private boolean selectPlacementBlock(LocalPlayer player, ClientLevel level) {
        int slot = findPlacementSlot(player, level);
        if (slot < 0) {
            return false;
        }
        Inventory inventory = player.getInventory();
        if (slot == inventory.getSelectedSlot()) {
            return true;
        }
        inventory.setSelectedSlot(slot);
        player.connection.send(new ServerboundSetCarriedItemPacket(slot));
        return true;
    }

    private int findPlacementSlot(LocalPlayer player, ClientLevel level) {
        Inventory inventory = player.getInventory();
        int selected = inventory.getSelectedSlot();
        if (isPlacementBlock(level, inventory.getItem(selected))) {
            return selected;
        }
        for (int i = 0; i < Inventory.getSelectionSize(); i++) {
            if (i != selected && isPlacementBlock(level, inventory.getItem(i))) {
                return i;
            }
        }
        return -1;
    }

    private boolean isPlacementBlock(ClientLevel level, ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof BlockItem blockItem)) {
            return false;
        }
        Block block = blockItem.getBlock();
        BlockState defaultState = block.defaultBlockState();
        if (defaultState.hasProperty(BlockStateProperties.WATERLOGGED)) {
            return false;
        }
        return defaultState.isCollisionShapeFullBlock(level, BlockPos.ZERO);
    }

    private void placeBlock(BlockPos pos, LocalPlayer player) {
        player.connection.send(new ServerboundUseItemOnPacket(
                InteractionHand.MAIN_HAND,
                placementHit(pos),
                1));
    }

    private void selectBestTool(BlockPos pos, LocalPlayer player, ClientLevel level) {
        BlockState state = level.getBlockState(pos);
        int originalSlot = player.getInventory().getSelectedSlot();
        float bestDelta = state.getDestroyProgress(player, level, pos);
        int bestSlot = originalSlot;
        if (bestDelta >= 1) {
            return;
        }
        for (int i = 0; i < 9; i++) {
            player.getInventory().setSelectedSlot(i);
            float delta = state.getDestroyProgress(player, level, pos);
            if (delta <= bestDelta) {
                continue;
            }
            bestSlot = i;
            bestDelta = delta;
        }
        player.getInventory().setSelectedSlot(originalSlot);
        if (bestSlot == originalSlot) {
            return;
        }
        player.getInventory().setSelectedSlot(bestSlot);
        player.connection.send(new ServerboundSetCarriedItemPacket(bestSlot));
    }

    private void registerCommand() {
        LiteralArgumentBuilder<FabricClientCommandSource> cmd = literal("eremovewater");
        cmd.executes(_ -> {
            toggle();
            return 1;
        });
        cmd.then(literal("toggle").executes(_ -> {
            toggle();
            return 1;
        }));
        cmd.then(literal("on").executes(_ -> {
            enabled = true;
            sendModMessage("RemoveWater enabled");
            return 1;
        }));
        cmd.then(literal("off").executes(_ -> {
            enabled = false;
            sendModMessage("RemoveWater disabled");
            return 1;
        }));
        cmd.then(literal("range")
                .executes(_ -> {
                    sendModMessage("RemoveWater range is " + range);
                    return 1;
                })
                .then(argument("range", DoubleArgumentType.doubleArg(1)).executes(c -> {
                    range = c.getArgument("range", Double.class);
                    sendModMessage("RemoveWater range set to " + range);
                    return 1;
                })));
        cmd.then(literal("rate")
                .executes(_ -> {
                    sendModMessage("RemoveWater rate is " + rate + " blocks per tick");
                    return 1;
                })
                .then(argument("rate", IntegerArgumentType.integer(1, 100)).executes(c -> {
                    rate = c.getArgument("rate", Integer.class);
                    sendModMessage("RemoveWater rate set to " + rate + " blocks per tick");
                    return 1;
                })));

        LiteralArgumentBuilder<FabricClientCommandSource> areaCmd = literal("area");
        BlockArea.commands((_, blockArea) -> {
            setArea(blockArea);
            useArea = true;
            sendModMessage("RemoveWater area updated");
        }).forEach(areaCmd::then);
        areaCmd.then(literal("clear").executes(_ -> {
            useArea = false;
            sendModMessage("RemoveWater area cleared, using the range around the player");
            return 1;
        }));
        cmd.then(areaCmd);

        register(cmd, "RemoveWater removes water by placing full blocks into it.");
    }

    private void toggle() {
        enabled = !enabled;
        sendModMessage(enabled ? "RemoveWater enabled" : "RemoveWater disabled");
    }

    private void setArea(BlockArea area) {
        if (area == null) {
            this.area = null;
            return;
        }
        if (area instanceof SavableBlockArea savableBlockArea) {
            this.area = savableBlockArea;
            return;
        }
        this.area = new SavableBlockArea(area);
    }

    private void onJoinWorld() {
        enabled = false;
        pendingPlacements.clear();
        breakTarget = null;
        breakStarted = false;
        breakDirection = null;
        breakProgress = 0.0F;
    }

    private void onConfigLoaded() {
        setArea(area);
    }
}
