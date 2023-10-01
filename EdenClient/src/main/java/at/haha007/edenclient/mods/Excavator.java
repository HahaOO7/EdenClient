package at.haha007.edenclient.mods;

import at.haha007.edenclient.annotations.Mod;
import at.haha007.edenclient.callbacks.ConfigLoadedCallback;
import at.haha007.edenclient.callbacks.JoinWorldCallback;
import at.haha007.edenclient.callbacks.PlayerTickCallback;
import at.haha007.edenclient.command.CommandManager;
import at.haha007.edenclient.utils.PlayerUtils;
import at.haha007.edenclient.utils.Scheduler;
import at.haha007.edenclient.utils.StringUtils;
import at.haha007.edenclient.utils.config.ConfigSubscriber;
import at.haha007.edenclient.utils.config.PerWorldConfig;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.commands.arguments.coordinates.Coordinates;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static at.haha007.edenclient.EdenClient.getMod;

@Mod
public class Excavator {
    private boolean enabled = false;
    @ConfigSubscriber("-1000000,1000000,-1000000,1000000,-1000000,1000000")
    private BoundingBox area;
    private BoundingBox wallArea;
    private Target target;

    private Excavator() {
        PerWorldConfig.get().register(this, "excavator");
        JoinWorldCallback.EVENT.register(this::onJoinWorld);
        PlayerTickCallback.EVENT.register(this::tick);
        ConfigLoadedCallback.EVENT.register(this::onConfigLoaded);
        registerCommand();
    }

    private void tick(LocalPlayer player) {
        if (!enabled) return;
        if (target == null) findTarget(player);
        if (target == null) {
            target = dropDownTarget(player);
            return;
        }
        if (target.tick(player))
            target = null;
    }

    private Target dropDownTarget(LocalPlayer player) {
        BlockPos playerPos = player.blockPosition();
        BlockPos dropFloor = playerPos.below().below();
        BlockState state = player.clientLevel.getBlockState(dropFloor);
        //if it is not safe to drop
        if (!state.getShape(player.clientLevel, dropFloor).isEmpty() &&
                !state.isFaceSturdy(player.clientLevel, dropFloor, Direction.UP)) {
            return new Target(dropFloor, 5, breakBlockTargetAction(dropFloor));
        }
        if (state.isFaceSturdy(player.clientLevel, dropFloor, Direction.UP)) {
            Vec3 center = Vec3.atBottomCenterOf(player.blockPosition());
            player.setPos(center);
            return new Target(dropFloor, 5, new BooleanSupplier() {
                int noActionDelay = 0;
                private final BooleanSupplier destroyBlock = breakBlockTargetAction(dropFloor.above());

                public boolean getAsBoolean() {
                    if (!destroyBlock.getAsBoolean()) return false;
                    return noActionDelay++ >= 10;
                }
            });
        }
        return new Target(playerPos, 4.5, placeBlockTargetAction(dropFloor));
    }

    private void findTarget(LocalPlayer player) {
        BlockPos playerPos = player.blockPosition();
        Optional<Target> target = streamOuterArea(playerPos.getY())
                .sorted(Comparator.comparingInt(x -> x.distManhattan(playerPos)))
                .map(x -> createTarget(x, player))
                .filter(Objects::nonNull)
                .findFirst();
        this.target = target.orElse(null);
    }

    private Target createTarget(Vec3i vec, LocalPlayer player) {
        if (!wallArea.isInside(vec)) return null;
        ClientLevel level = player.clientLevel;
        BlockPos posFeet = new BlockPos(vec);
        BlockPos posFloor = posFeet.below();
        BlockPos posHead = posFeet.above();
        BlockState blockStateFeet = level.getBlockState(posFeet);
        FluidState fluidStateFeet = level.getFluidState(posFeet);
        BlockState blockStateFloor = level.getBlockState(posFloor);
        FluidState fluidStateFloor = level.getFluidState(posFloor);
        BlockState blockStateHead = level.getBlockState(posHead);
        FluidState fluidStateHead = level.getFluidState(posHead);

        //check for fluids to replace
        if (!fluidStateFeet.isEmpty() && (blockStateFeet.canBeReplaced() || blockStateFeet.isAir())) {
            return new Target(posFeet, 4.5, placeBlockTargetAction(posFeet));
        }
        if (!fluidStateFloor.isEmpty() && (blockStateFloor.canBeReplaced() || blockStateFloor.isAir())) {
            return new Target(posFloor, 4.5, placeBlockTargetAction(posFloor));
        }
        if (!fluidStateHead.isEmpty() && (blockStateHead.canBeReplaced() || blockStateHead.isAir())) {
            return new Target(posHead, 4.5, placeBlockTargetAction(posHead));
        }

        //check for floor to place
        if (blockStateFloor.canBeReplaced() || blockStateFloor.isAir()) {
            return new Target(posFloor, 4.5, placeBlockTargetAction(posFloor));
        }

        //if wall place blocks
        if (isWall(posFeet)) {
            if (blockStateFeet.canBeReplaced() || blockStateFeet.isAir()) {
                return new Target(posFeet, 4.5, placeBlockTargetAction(posFeet));
            }
            if (blockStateFloor.canBeReplaced() || blockStateFloor.isAir()) {
                return new Target(posFloor, 4.5, placeBlockTargetAction(posFloor));
            }
            if (blockStateHead.canBeReplaced() || blockStateHead.isAir()) {
                return new Target(posHead, 4.5, placeBlockTargetAction(posHead));
            }
            return null;
        }

        BlockPos[] blocks = new BlockPos[]{posFeet, posHead};
        for (BlockPos block : blocks) {
            //check for neighboring fluids
            if (hasNeighboringFluids(block, level)) {
                continue;
            }
            //check for blocks to be broken
            if (level.getBlockState(block).getShape(level, block).isEmpty()) {
                continue;
            }
            return new Target(block, 5, breakBlockTargetAction(block));
        }

        return null;
    }

    @SuppressWarnings("RedundantIfStatement")
    private boolean hasNeighboringFluids(BlockPos pos, ClientLevel level) {
        if (!level.getFluidState(pos.above()).isEmpty()) return true;
        if (!level.getFluidState(pos.north()).isEmpty()) return true;
        if (!level.getFluidState(pos.south()).isEmpty()) return true;
        if (!level.getFluidState(pos.east()).isEmpty()) return true;
        if (!level.getFluidState(pos.west()).isEmpty()) return true;
        return false;
    }

    private BooleanSupplier breakBlockTargetAction(BlockPos pos) {
        return () -> {
            LocalPlayer player = PlayerUtils.getPlayer();
            ClientLevel level = player.clientLevel;
            BlockState state = level.getBlockState(pos);
            if (state.getShape(level, pos).isEmpty()) return true;
            selectBestTool(pos, player);
            return PlayerUtils.breakBlock(pos);
        };
    }

    private BooleanSupplier placeBlockTargetAction(BlockPos pos) {
        return () -> {
            if (!PlayerUtils.selectPlacableBlock()) {
                return false;
            }
            MultiPlayerGameMode gameMode = Minecraft.getInstance().gameMode;
            if (gameMode == null) {
                return false;
            }
            LocalPlayer player = PlayerUtils.getPlayer();
            BlockHitResult hitResult = new BlockHitResult(Vec3.atLowerCornerOf(pos), Direction.UP, pos, false);
            gameMode.useItemOn(player, InteractionHand.MAIN_HAND, hitResult);
            return true;
        };
    }

    private void selectBestTool(BlockPos pos, LocalPlayer player) {
        ClientLevel level = player.clientLevel;
        BlockState state = level.getBlockState(pos);
        int originalSlot = player.getInventory().selected;
        float bestDelta = state.getDestroyProgress(player, level, pos);
        int bestSlot = originalSlot;
        if (bestDelta >= 1) return;
        for (int i = 0; i < 9; i++) {
            player.getInventory().selected = i;
            float delta = state.getDestroyProgress(player, level, pos);
            if (delta <= bestDelta) continue;
            bestSlot = i;
            bestDelta = delta;
        }
        player.getInventory().selected = originalSlot;
        if (bestSlot == originalSlot) return;
        player.getInventory().selected = bestSlot;
        player.connection.send(new ServerboundSetCarriedItemPacket(bestSlot));
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

    private Stream<Vec3i> streamOuterArea(int y) {
        int zSpan = wallArea.getZSpan();
        int xSpan = wallArea.getXSpan();
        Vec3i src = new Vec3i(wallArea.minX(), y, wallArea.minZ());
        Vec3i[] vectors = new Vec3i[xSpan * zSpan];
        for (int x = 0; x < xSpan; x++) {
            for (int z = 0; z < zSpan; z++) {
                vectors[x + z * xSpan] = new Vec3i(x, 0, z).offset(src);
            }
        }
        return Arrays.stream(vectors);
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
                        try {
                            world.setBlockAndUpdate(b, Blocks.WATER.defaultBlockState());
                            Thread.sleep(1);
                        } catch (InterruptedException | IndexOutOfBoundsException e) {
                            StringUtils.getLogger().error("Error while don't-ing.", e);
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
            target = null;
            return 1;
        });
        ///eexcavate ~ ~10 ~ ~10 ~-100 ~10
        toggle.executes(c -> {
            enabled = !enabled;
            if (enabled) PlayerUtils.sendModMessage("Excavator enabled.");
            else PlayerUtils.sendModMessage("Excavator disabled.");
            target = null;
            return 1;
        });
        pos2Cmd.executes(c -> {
            Player player = PlayerUtils.getPlayer();
            CommandSourceStack stack = player.createCommandSourceStack();
            BlockPos pos1 = c.getArgument("pos1", Coordinates.class).getBlockPos(stack);
            BlockPos pos2 = c.getArgument("pos2", Coordinates.class).getBlockPos(stack);
            setArea(BoundingBox.fromCorners(pos1, pos2));
            PlayerUtils.sendModMessage("Excavating from %s to %s.".formatted(pos1, pos2));
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
        target = null;
    }

    private void onConfigLoaded() {
        setArea(area);
    }

    private record Target(BlockPos pos, double actionDistance, BooleanSupplier action) {
        public boolean tick(LocalPlayer player) {
            if (player.position().subtract(pos.getCenter()).horizontalDistance() > 2)
                PlayerUtils.walkTowards(pos);
            return performAction(player);
        }

        private boolean performAction(LocalPlayer player) {
            if (pos.getCenter().distanceTo(player.position()) > actionDistance) return false;
            return action.getAsBoolean();
        }
    }
}
