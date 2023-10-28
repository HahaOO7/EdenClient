package at.haha007.edenclient.mods;

import at.haha007.edenclient.annotations.Mod;
import at.haha007.edenclient.callbacks.ConfigLoadedCallback;
import at.haha007.edenclient.callbacks.JoinWorldCallback;
import at.haha007.edenclient.callbacks.PlayerTickCallback;
import at.haha007.edenclient.command.CommandManager;
import at.haha007.edenclient.utils.PlayerUtils;
import at.haha007.edenclient.utils.Scheduler;
import at.haha007.edenclient.utils.Utils;
import at.haha007.edenclient.utils.area.BlockArea;
import at.haha007.edenclient.utils.area.SavableBlockArea;
import at.haha007.edenclient.utils.config.ConfigSubscriber;
import at.haha007.edenclient.utils.config.PerWorldConfig;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static at.haha007.edenclient.EdenClient.getMod;

@Mod
public class Excavator {
    private boolean enabled = false;
    @ConfigSubscriber()
    private SavableBlockArea area;
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
        if (area == null) {
            PlayerUtils.sendModMessage("Excavator area not defined");
            enabled = false;
            return;
        }
        if (target == null) {
            target = findTarget(player);
        }
        if (target == null) {
            target = dropDownTarget(player);
            if (target == null) {
                enabled = false;
                PlayerUtils.sendModMessage("Excavator done");
            }
            return;
        }
        if (target.tick(player))
            target = null;
    }

    private Target dropDownTarget(LocalPlayer player) {
        BlockPos playerPos = player.blockPosition();
        int floorY = playerPos.getY() - 1;
        BlockPos dropPos = area.stream().filter(p -> p.getY() == floorY)
                .min(Comparator.comparingDouble(pos -> pos.distSqr(playerPos)))
                .map(BlockPos::above)
                .orElse(null);
        if (dropPos == null) return null;
        BlockPos dropFloor = dropPos.below().below();
        BlockState state = player.clientLevel.getBlockState(dropFloor);
        //if it is not safe to drop
        if (!state.getShape(player.clientLevel, dropFloor).isEmpty() &&
                !state.isFaceSturdy(player.clientLevel, dropFloor, Direction.UP)) {
            return new Target(dropFloor, 5, breakBlockTargetAction(dropFloor));
        }
        if (state.isFaceSturdy(player.clientLevel, dropFloor, Direction.UP)) {
            Vec3 center = Vec3.atBottomCenterOf(playerPos);

            return new Target(dropFloor, 5, new BooleanSupplier() {
                int noActionDelay = 0;
                private final BooleanSupplier destroyBlock = breakBlockTargetAction(dropFloor.above());

                public boolean getAsBoolean() {
                    if (!destroyBlock.getAsBoolean()) return false;
                    return noActionDelay++ >= 10;
                }
            }) {
                @Override
                public boolean tick(LocalPlayer player) {
                    PlayerUtils.walkTowards(pos());
                    if (Vec3.atBottomCenterOf(pos()).subtract(player.position()).horizontalDistance() < .3) {
                        player.setPos(center);
                        return super.performAction(player);
                    }
                    return false;
                }
            };
        }
        return new Target(dropPos, 4.5, placeBlockTargetAction(dropFloor));
    }

    private Target findTarget(LocalPlayer player) {
        BlockPos playerPos = player.blockPosition();
        Target target = areaTarget(playerPos);
        if (target != null) return target;
        target = ceilingTarget(playerPos);
        if (target != null) return target;
        target = floorTarget(playerPos);
        if (target != null) return target;
        target = wallTarget(playerPos);
        return target;
    }

    private Target areaTarget(BlockPos pos) {
        int minY = pos.getY() - 1;
        int maxY = pos.getY() + 4;
        List<BlockPos> c = area.stream().filter(p -> filterMinY(minY, p))
                .filter(p -> p.getY() < maxY)
                .sorted(Comparator.comparingDouble(pos::distManhattan)).toList();

        Optional<Target> placeTarget = c.stream().map(this::placeTarget).filter(Objects::nonNull).findFirst();
        c = c.stream().filter(p -> p.getY() > minY).toList();
        Optional<Target> breakTarget = c.stream().map(this::breakTarget).filter(Objects::nonNull).findFirst();

        double distBreak = breakTarget.map(b -> b.pos().distSqr(pos)).orElse(Double.MAX_VALUE);
        double distPlace = placeTarget.map(b -> b.pos().distSqr(pos)).orElse(Double.MAX_VALUE) - 2;
        return distBreak < distPlace ? breakTarget.orElse(null) : placeTarget.orElse(null);
    }

    private Target ceilingTarget(BlockPos playerPos) {
        int minY = playerPos.getY() + 2;
        int maxY = playerPos.getY() + 4;
        List<BlockPos> c = area.ceilingStream().filter(p -> filterMinY(minY, p))
                .filter(p -> p.getY() < maxY)
                .sorted(Comparator.comparingDouble(playerPos::distManhattan)).toList();
        Optional<Target> target = c.stream().map(this::breakWaterloggedTarget).filter(Objects::nonNull).findFirst();
        if (target.isPresent()) return target.get();

        target = c.stream().map(this::placeTarget).filter(Objects::nonNull).findFirst();
        return target.orElse(null);
    }

    private Target floorTarget(BlockPos playerPos) {
        int minY = playerPos.getY() - 2;
        int maxY = playerPos.getY() + 2;
        List<BlockPos> c = area.floorStream().filter(p -> filterMinY(minY, p))
                .filter(p -> p.getY() < maxY)
                .sorted(Comparator.comparingDouble(playerPos::distManhattan)).toList();
        Optional<Target> target = c.stream().map(this::breakWaterloggedTarget).filter(Objects::nonNull).findFirst();
        if (target.isPresent()) return target.get();

        target = c.stream().map(this::placeTarget).filter(Objects::nonNull).findFirst();
        return target.orElse(null);
    }

    private Target wallTarget(BlockPos playerPos) {
        int minY = playerPos.getY();
        int maxY = playerPos.getY() + 2;
        List<BlockPos> c = area.wallStream().filter(p -> filterMinY(minY, p))
                .filter(p -> p.getY() < maxY)
                .sorted(Comparator.comparingDouble(playerPos::distManhattan)).toList();
        Optional<Target> target = c.stream().map(this::breakWaterloggedTarget).filter(Objects::nonNull).findFirst();
        if (target.isPresent()) return target.get();

        target = c.stream().map(this::placeTarget).filter(Objects::nonNull).findFirst();
        return target.orElse(null);
    }

    private boolean filterMinY(int y, BlockPos pos) {
        return pos.getY() >= y;
    }

    private Target placeTarget(BlockPos pos) {
        LocalPlayer player = PlayerUtils.getPlayer();
        ClientLevel level = player.clientLevel;
        Target target = new Target(pos, 4.5, placeBlockTargetAction(pos));
        BlockState blockState = level.getBlockState(pos);
        FluidState fluidState = level.getFluidState(pos);
        int floorY = player.getBlockY() - 1;
        if (!fluidState.isEmpty() && blockState.getShape(level, pos).isEmpty()) {
            return target;
        }
        if ((blockState.isAir() || blockState.canBeReplaced()) && pos.getY() == floorY) {
            return target;
        }
        if ((area.isWall(pos) || area.isCeiling(pos) || area.isFloor(pos)) && (blockState.isAir() || blockState.canBeReplaced())) {
            return target;
        }
        return null;
    }

    private Target breakTarget(BlockPos pos) {
        LocalPlayer player = PlayerUtils.getPlayer();
        ClientLevel level = player.clientLevel;
        BlockState blockState = level.getBlockState(pos);
        FluidState fluidState = level.getFluidState(pos);
        Target target = new Target(pos, 5, breakBlockTargetAction(pos));
        boolean hasCollisionShape = !blockState.getShape(level, pos).isEmpty();
        if (!fluidState.isEmpty() && hasCollisionShape)
            return target;
        if (hasNeighboringFluids(pos, level))
            return null;
        if (hasCollisionShape)
            return target;
        return null;
    }

    private Target breakWaterloggedTarget(BlockPos pos) {
        LocalPlayer player = PlayerUtils.getPlayer();
        ClientLevel level = player.clientLevel;
        BlockState blockState = level.getBlockState(pos);
        FluidState fluidState = level.getFluidState(pos);
        Target target = new Target(pos, 5, breakBlockTargetAction(pos));
        boolean hasCollisionShape = !blockState.getShape(level, pos).isEmpty();
        if (!fluidState.isEmpty() && hasCollisionShape)
            return target;
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
                            Utils.getLogger().error("Error while don't-ing.", e);
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

        cmd.then(off);
        cmd.then(on);
        cmd.then(toggle);

        LiteralArgumentBuilder<ClientSuggestionProvider> areaCmd = CommandManager.literal("area");
        var cmds = BlockArea.commands((c, a) -> {
            setArea(a);
            PlayerUtils.sendModMessage("Updated area. Use '/eexcavate on' to start.");
        });
        cmds.forEach(areaCmd::then);
        cmd.then(areaCmd);

        CommandManager.register(cmd, "Excavate area.");
    }

    private void setArea(BlockArea area) {
        this.area = new SavableBlockArea(area);
    }

    private void onJoinWorld() {
        enabled = false;
        target = null;
    }

    private void onConfigLoaded() {
        setArea(area);
    }

    private static class Target {
        private final BlockPos pos;
        private final double actionDistance;
        private final BooleanSupplier action;

        private Target(BlockPos pos, double actionDistance, BooleanSupplier action) {
            this.pos = pos;
            this.actionDistance = actionDistance;
            this.action = action;
        }

        public boolean tick(LocalPlayer player) {
            if (player.position().subtract(pos.getCenter()).horizontalDistance() > 2)
                PlayerUtils.walkTowards(pos);
            return performAction(player);
        }

        private boolean performAction(LocalPlayer player) {
            if (pos.getCenter().distanceTo(player.position()) > actionDistance) return false;
            return action.getAsBoolean();
        }

        public BlockPos pos() {
            return pos;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (obj == null || obj.getClass() != this.getClass()) return false;
            var that = (Target) obj;
            return Objects.equals(this.pos, that.pos) &&
                    Double.doubleToLongBits(this.actionDistance) == Double.doubleToLongBits(that.actionDistance) &&
                    Objects.equals(this.action, that.action);
        }

        @Override
        public int hashCode() {
            return Objects.hash(pos, actionDistance, action);
        }

        @Override
        public String toString() {
            return "Target[" +
                    "pos=" + pos + ", " +
                    "actionDistance=" + actionDistance + ", " +
                    "action=" + action + ']';
        }

    }
}
