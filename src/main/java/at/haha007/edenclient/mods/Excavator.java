package at.haha007.edenclient.mods;

import at.haha007.edenclient.Mod;
import at.haha007.edenclient.callbacks.JoinWorldCallback;
import at.haha007.edenclient.callbacks.PlayerTickCallback;
import at.haha007.edenclient.command.CommandManager;
import at.haha007.edenclient.utils.PlayerUtils;
import at.haha007.edenclient.utils.Scheduler;
import at.haha007.edenclient.utils.config.ConfigSubscriber;
import at.haha007.edenclient.utils.config.PerWorldConfig;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.commands.arguments.coordinates.Coordinates;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static at.haha007.edenclient.EdenClient.getMod;
import static net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK;

@Mod
public class Excavator {
    private boolean enabled = false;
    @ConfigSubscriber("3")
    private int limit;
    @ConfigSubscriber("-1000000,1000000,-1000000,1000000,-1000000,1000000")
    private BoundingBox area;
    private BoundingBox wallArea;
    private BlockPos currentlyBreaking;

    private Excavator() {
        PerWorldConfig.get().register(this, "excavator");
        JoinWorldCallback.EVENT.register(this::onJoinWorld);
        PlayerTickCallback.EVENT.register(this::tick);
        streamOut(Vec3i.ZERO).limit(100).forEach(System.out::println);
        registerCommand();
    }

    private void tick(LocalPlayer localPlayer) {
        if (!enabled) return;
        move();
        if (tryToPlaceBlocks()) return;
        tryToBreakBlocks();
    }

    private void move() {
        LocalPlayer player = PlayerUtils.getPlayer();
        Optional<Vec3i> target = getNearestTarget(player.blockPosition());
        if (target.isEmpty())
            PlayerUtils.walkTowards(player.blockPosition().getCenter());
        else
            PlayerUtils.walkTowards(Vec3.atBottomCenterOf(target.get()));
    }

    /**
     * @return true if at least one block was placed
     */
    private boolean tryToPlaceBlocks() {
        LocalPlayer player = PlayerUtils.getPlayer();
        ClientLevel level = player.clientLevel;
        BlockPos playerPos = player.blockPosition();
        MultiPlayerGameMode gameMode = Minecraft.getInstance().gameMode;
        if (gameMode == null) return false;
        AtomicInteger placed = new AtomicInteger(0);
        getNearbyAreaPositions(playerPos, 4.5)
                .map(BlockPos::new)
                .filter(pos -> pos.getY() == playerPos.below().getY())
                .filter(pos -> level.getBlockState(pos).isAir() ||
                        level.getBlockState(pos).is(Blocks.WATER) ||
                        level.getBlockState(pos).is(Blocks.LAVA))
                .limit((limit - placed.get()) / 2)
                .forEach(pos -> {
                    PlayerUtils.selectPlacableBlock();
                    Direction dir = Direction.UP;
                    BlockHitResult hitResult = new BlockHitResult(Vec3.atBottomCenterOf((pos.relative(dir))), dir, pos, false);
                    gameMode.useItemOn(player, InteractionHand.MAIN_HAND, hitResult);
                    placed.addAndGet(2);
                });
        getNearbyWallPositions(playerPos, 4.5)
                .map(BlockPos::new)
                .filter(pos -> !level.getFluidState(pos).isEmpty())
                .filter(pos -> level.getBlockState(pos).isAir() ||
                        level.getBlockState(pos).is(Blocks.WATER) ||
                        level.getBlockState(pos).is(Blocks.LAVA))
                .limit((limit - placed.get()) / 2)
                .forEach(pos -> {
                    PlayerUtils.selectPlacableBlock();
                    Direction dir = Direction.UP;
                    BlockHitResult hitResult = new BlockHitResult(Vec3.atBottomCenterOf((pos.relative(dir))), dir, pos, false);
                    gameMode.useItemOn(player, InteractionHand.MAIN_HAND, hitResult);
                    placed.addAndGet(2);
                });
        if (limit == placed.get()) return true;
        getNearbyAreaPositions(playerPos, 4.5)
                .map(BlockPos::new)
                .filter(pos -> !level.getFluidState(pos).isEmpty())
                .filter(pos -> level.getBlockState(pos).isAir() ||
                        level.getBlockState(pos).is(Blocks.WATER) ||
                        level.getBlockState(pos).is(Blocks.LAVA))
                .limit((limit - placed.get()) / 2)
                .forEach(pos -> {
                    PlayerUtils.selectPlacableBlock();
                    Direction dir = Direction.UP;
                    BlockHitResult hitResult = new BlockHitResult(Vec3.atBottomCenterOf((pos.relative(dir))), dir, pos, false);
                    gameMode.useItemOn(player, InteractionHand.MAIN_HAND, hitResult);
                    placed.addAndGet(2);
                });
        return placed.get() > 0;
    }

    /**
     * @return true if hitting a block
     */
    private boolean tryToBreakBlocks() {
        LocalPlayer player = PlayerUtils.getPlayer();
        ClientLevel level = player.clientLevel;
        BlockPos playerPos = player.blockPosition();
        ClientPacketListener packetListener = player.connection;
        //try to continue block breaking
        if (currentlyBreaking != null && level.getBlockState(currentlyBreaking).getCollisionShape(level, currentlyBreaking).isEmpty())
            currentlyBreaking = null;
        if (currentlyBreaking != null && playerPos.distSqr(currentlyBreaking) < 25) {
            swapToBestSlot(currentlyBreaking, player);
            if (!PlayerUtils.breakBlock(currentlyBreaking)) return true;
            currentlyBreaking = null;
            return true;
        }
        AtomicInteger actions = new AtomicInteger(0);
        //find waterlogged floor blocks
        getNearbyAreaPositions(playerPos, 4.5)
                .map(BlockPos::new)
                .filter(pos -> !level.getFluidState(pos).isEmpty())
                .filter(pos -> !level.getBlockState(pos).getCollisionShape(level, pos).isEmpty())
                .limit(Math.max(limit - actions.get(),0))
                .forEach(pos -> {
                    if (actions.get() >= limit) return;
                    if (swapToBestSlot(pos, player))
                        actions.incrementAndGet();
                    if (isInstantBreakable(pos, player)) {
                        actions.incrementAndGet();
                        Direction hitDirectionForBlock = PlayerUtils.getHitDirectionForBlock(player, pos);
                        ServerboundPlayerActionPacket packet = new ServerboundPlayerActionPacket(START_DESTROY_BLOCK, pos, hitDirectionForBlock);
                        packetListener.send(packet);
                        player.clientLevel.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
                    }
                    currentlyBreaking = pos;
                    actions.set(20000000);
                });
        getNearbyAreaPositions(playerPos, 4.5)
                .map(BlockPos::new)
                .map(BlockPos::above)
                .filter(pos -> !level.getFluidState(pos).isEmpty())
                .filter(pos -> !level.getBlockState(pos).getCollisionShape(level, pos).isEmpty())
                .limit(Math.max(limit - actions.get(),0))
                .forEach(pos -> {
                    if (actions.get() >= limit) return;
                    if (swapToBestSlot(pos, player))
                        actions.incrementAndGet();
                    if (isInstantBreakable(pos, player)) {
                        actions.incrementAndGet();
                        Direction hitDirectionForBlock = PlayerUtils.getHitDirectionForBlock(player, pos);
                        ServerboundPlayerActionPacket packet = new ServerboundPlayerActionPacket(START_DESTROY_BLOCK, pos, hitDirectionForBlock);
                        packetListener.send(packet);
                        player.clientLevel.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
                    }
                    currentlyBreaking = pos;
                    actions.set(20000000);
                });

        //find waterlogged wall blocks
        getNearbyWallPositions(playerPos, 4.5)
                .map(BlockPos::new)
                .filter(pos -> !level.getFluidState(pos).isEmpty())
                .filter(pos -> !level.getBlockState(pos).getCollisionShape(level, pos).isEmpty())
                .limit(Math.max(limit - actions.get(),0))
                .forEach(pos -> {
                    if (actions.get() >= limit) return;
                    if (swapToBestSlot(pos, player))
                        actions.incrementAndGet();
                    if (isInstantBreakable(pos, player)) {
                        actions.incrementAndGet();
                        Direction hitDirectionForBlock = PlayerUtils.getHitDirectionForBlock(player, pos);
                        ServerboundPlayerActionPacket packet = new ServerboundPlayerActionPacket(START_DESTROY_BLOCK, pos, hitDirectionForBlock);
                        packetListener.send(packet);
                        player.clientLevel.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
                    }
                    currentlyBreaking = pos;
                    actions.set(20000000);
                });
        getNearbyWallPositions(playerPos, 4.5)
                .map(BlockPos::new)
                .map(BlockPos::above)
                .filter(pos -> !level.getFluidState(pos).isEmpty())
                .filter(pos -> !level.getBlockState(pos).getCollisionShape(level, pos).isEmpty())
                .limit(Math.max(limit - actions.get(),0))
                .forEach(pos -> {
                    if (actions.get() >= limit) return;
                    if (swapToBestSlot(pos, player))
                        actions.incrementAndGet();
                    if (isInstantBreakable(pos, player)) {
                        actions.incrementAndGet();
                        Direction hitDirectionForBlock = PlayerUtils.getHitDirectionForBlock(player, pos);
                        ServerboundPlayerActionPacket packet = new ServerboundPlayerActionPacket(START_DESTROY_BLOCK, pos, hitDirectionForBlock);
                        packetListener.send(packet);
                        player.clientLevel.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
                    }
                    currentlyBreaking = pos;
                    actions.set(20000000);
                });
        //find area block to break
        getNearbyAreaPositions(playerPos, 4.5)
                .map(BlockPos::new)
                .filter(pos -> level.getFluidState(pos.above()).isEmpty())
                .filter(pos -> level.getFluidState(pos.north()).isEmpty())
                .filter(pos -> level.getFluidState(pos.south()).isEmpty())
                .filter(pos -> level.getFluidState(pos.east()).isEmpty())
                .filter(pos -> level.getFluidState(pos.west()).isEmpty())
                .filter(pos -> !level.getBlockState(pos).getCollisionShape(level, pos).isEmpty())
                .limit(Math.max(limit - actions.get(),0))
                .forEach(pos -> {
                    if (actions.get() >= limit) return;
                    if (swapToBestSlot(pos, player))
                        actions.incrementAndGet();
                    if (isInstantBreakable(pos, player)) {
                        actions.incrementAndGet();
                        Direction hitDirectionForBlock = PlayerUtils.getHitDirectionForBlock(player, pos);
                        ServerboundPlayerActionPacket packet = new ServerboundPlayerActionPacket(START_DESTROY_BLOCK, pos, hitDirectionForBlock);
                        packetListener.send(packet);
                        player.clientLevel.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
                    }
                    currentlyBreaking = pos;
                    actions.set(20000000);
                });
        getNearbyAreaPositions(playerPos, 4.5)
                .map(BlockPos::new)
                .map(BlockPos::above)
                .filter(pos -> level.getFluidState(pos.above()).isEmpty())
                .filter(pos -> level.getFluidState(pos.north()).isEmpty())
                .filter(pos -> level.getFluidState(pos.south()).isEmpty())
                .filter(pos -> level.getFluidState(pos.east()).isEmpty())
                .filter(pos -> level.getFluidState(pos.west()).isEmpty())
                .filter(pos -> !level.getBlockState(pos).getCollisionShape(level, pos).isEmpty())
                .limit(Math.max(limit - actions.get(),0))
                .forEach(pos -> {
                    if (actions.get() >= limit) return;
                    if (swapToBestSlot(pos, player))
                        actions.incrementAndGet();
                    if (isInstantBreakable(pos, player)) {
                        actions.incrementAndGet();
                        Direction hitDirectionForBlock = PlayerUtils.getHitDirectionForBlock(player, pos);
                        ServerboundPlayerActionPacket packet = new ServerboundPlayerActionPacket(START_DESTROY_BLOCK, pos, hitDirectionForBlock);
                        packetListener.send(packet);
                        player.clientLevel.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
                    }
                    currentlyBreaking = pos;
                    actions.set(20000000);
                });
        return actions.get() > 0;
    }

    private Stream<Vec3i> getNearbyWallPositions(Vec3i center, double distance) {
        int radius = (int) Math.ceil(distance);
        long area = radius * radius * 4L;
        double distanceSquared = distance * distance;
        return streamOut(center)
                .limit(area)
                .filter(this::isWall)
                .filter(v -> center.distSqr(v) <= distanceSquared);
    }

    private Stream<Vec3i> getNearbyAreaPositions(Vec3i center, double distance) {
        int radius = (int) Math.ceil(distance);
        long area = radius * radius * 4L;
        double distanceSquared = distance * distance;
        return streamOut(center)
                .limit(area)
                .filter(this.area::isInside)
                .filter(v -> center.distSqr(v) <= distanceSquared);
    }

    private Optional<Vec3i> getNearestTarget(Vec3i center) {
        long limit = (long) area.getXSpan() * area.getZSpan();
        return streamOut(center)
                .limit(limit)
                .filter(this::isTarget)
                .findFirst();
    }

    private boolean isInstantBreakable(BlockPos pos, LocalPlayer player) {
        ClientLevel world = player.clientLevel;
        BlockState state = world.getBlockState(pos);
        float delta = state.getDestroyProgress(player, world, pos);
        return delta >= 1;
    }

    /**
     * @return true if swapped
     */
    private boolean swapToBestSlot(BlockPos pos, LocalPlayer player) {
        Inventory inventory = player.getInventory();
        ClientLevel world = player.clientLevel;
        BlockState state = world.getBlockState(pos);
        int startSlot = inventory.selected;
        int bestSlot = startSlot;
        float bestDelta = state.getDestroyProgress(player, world, pos);
        //if the selected item is already instant breaking there is no need to find the best one
        if (bestDelta >= 1) {
//            System.out.println("Starter slot is instant breaking.");
            return false;
        }
        for (int i = 0; i < 9; i++) {
            float delta = state.getDestroyProgress(player, world, pos);
            if (delta >= bestDelta) continue;
            bestSlot = i;
            bestDelta = delta;
        }
        inventory.selected = startSlot;
        if (startSlot == bestSlot) return false;
        inventory.selected = bestSlot;
        PlayerUtils.packetListener().send(new ServerboundSetCarriedItemPacket(bestSlot));
        return true;
    }

    private boolean isTarget(Vec3i vec) {
        return isWallTarget(vec) || isAreaTarget(vec);
    }

    private boolean isWallTarget(Vec3i vec) {
        if (area.isInside(vec) || !wallArea.isInside(vec)) return false;
        LocalPlayer player = PlayerUtils.getPlayer();
        ClientLevel world = player.clientLevel;
        BlockPos feet = new BlockPos(vec);
        BlockPos head = new BlockPos(vec.above());
        BlockPos floor = new BlockPos(vec.below());

        if (!world.getFluidState(floor).isEmpty())
            return true;
        if (!world.getFluidState(feet).isEmpty())
            return true;
        return !world.getFluidState(head).isEmpty();
    }

    private boolean isAreaTarget(Vec3i vec) {
        if (!area.isInside(vec)) return false;
        LocalPlayer player = PlayerUtils.getPlayer();
        ClientLevel world = player.clientLevel;
        BlockPos floor = new BlockPos(vec.below());
        BlockPos feet = new BlockPos(vec);
        BlockPos head = new BlockPos(vec.above());

        if (!world.getFluidState(floor).isEmpty())
            return true;
        if (!world.getBlockState(feet).isAir())
            return true;
        return !world.getBlockState(head).isAir();
    }

    private boolean isWall(Vec3i pos) {
        return wallArea.isInside(pos) && !area.isInside(pos);
    }

    private Stream<Vec3i> streamOut(Vec3i source) {
        return Stream.generate(new Supplier<>() {
            private Vec3i last = source.relative(Direction.WEST);
            private long maxI = -2;
            private long i = 0;
            private Direction direction = Direction.NORTH;

            @Override
            public Vec3i get() {
                if (i++ >= maxI / 2) {
                    direction = direction.getClockWise();
                    i = 0;
                    maxI++;
                }
                Vec3i vec = last.relative(direction);
                last = vec;
                return vec;
            }
        });
    }

    private void registerCommand() {
        LiteralArgumentBuilder<ClientSuggestionProvider> cmd = CommandManager.literal("eexcavate");
        var pos1Cmd = CommandManager.argument("pos1", BlockPosArgument.blockPos());
        var pos2Cmd = CommandManager.argument("pos2", BlockPosArgument.blockPos());
        var off = CommandManager.literal("off");
        var on = CommandManager.literal("on");
        var toggle = CommandManager.literal("toggle");

        cmd.then(CommandManager.literal("don't").executes(c -> {
            Scheduler scheduler = getMod(Scheduler.class);
            ClientLevel world = PlayerUtils.getPlayer().clientLevel;
            scheduler.runAsync(
                    () -> streamOut(PlayerUtils.getPlayer().blockPosition().below()).map(BlockPos::new).forEach(b -> {
                        world.setBlockAndUpdate(b, Blocks.WATER.defaultBlockState());
                        try {
                            Thread.sleep(1);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }));
            return 1;
        }));

        cmd.executes(c -> {
            PlayerUtils.sendModMessage("/eexcavate <from> <to>");
            PlayerUtils.sendModMessage("/eexcavate off");
            PlayerUtils.sendModMessage("/eexcavate on");
            PlayerUtils.sendModMessage("/eexcavate toggle");
            return 1;
        });
        off.executes(c -> {
            enabled = false;
            PlayerUtils.sendModMessage("Excavator disabled.");
            return 1;
        });
        on.executes(c -> {
            enabled = true;
            PlayerUtils.sendModMessage("Excavator enabled.");
            return 1;
        });
        ///eexcavate ~ ~10 ~ ~10 ~-100 ~10
        toggle.executes(c -> {
            enabled = !enabled;
            if (enabled) PlayerUtils.sendModMessage("Excavator enabled.");
            else PlayerUtils.sendModMessage("Excavator disabled.");
            return 1;
        });
        pos2Cmd.executes(c -> {
            Player player = PlayerUtils.getPlayer();
            CommandSourceStack stack = player.createCommandSourceStack();
            BlockPos pos1 = c.getArgument("pos1", Coordinates.class).getBlockPos(stack);
            BlockPos pos2 = c.getArgument("pos2", Coordinates.class).getBlockPos(stack);
            setArea(BoundingBox.fromCorners(pos1, pos2));
            PlayerUtils.sendModMessage("Excavating from %s to %s.");
            return 1;
        });

        cmd.then(off);
        cmd.then(on);
        cmd.then(toggle);
        pos1Cmd.then(pos2Cmd);
        cmd.then(pos1Cmd);

        CommandManager.register(cmd, "Excavate area.");
    }

    private void setArea(BoundingBox box) {
        area = box;
        wallArea = area.inflatedBy(1);
    }

    private void onJoinWorld() {
        enabled = false;
        currentlyBreaking = null;
        getMod(Scheduler.class).scheduleSyncDelayed(() -> {
            setArea(area);
        }, 1);
    }
}
