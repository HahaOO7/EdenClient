package at.haha007.edenclient.utils.pathing.segmentcalculator;

import at.haha007.edenclient.utils.PlayerUtils;
import at.haha007.edenclient.utils.pathing.PathingUtils;
import at.haha007.edenclient.utils.pathing.segment.PathSegment;
import at.haha007.edenclient.utils.pathing.segment.StraightPathSegment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public class StraightSegmentCalculator implements SegmentCalculator {
    private final double maxDropDistance;

    public StraightSegmentCalculator(double maxDropDistance) {
        this.maxDropDistance = maxDropDistance;
    }

    @Override
    public Collection<PathSegment> calculateSegments(Vec3 from) {
        BlockPos fromBlockPos = new BlockPos(Mth.floor(from.x), Mth.floor(from.y), Mth.floor(from.z));
        LocalPlayer player = PlayerUtils.getPlayer();
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return List.of();
        }
        double maxStepUp = player.maxUpStep();
        double playerHeight = player.getDimensions(player.getPose()).height();
        BlockPos fromHeight = fromBlockPos.above();
        for (int y = Mth.floor(from.y); y <= Mth.floor(from.y + playerHeight + maxStepUp); y++) {
            BlockState blockState = level.getBlockState(fromHeight.above(y - fromHeight.getY()));
            VoxelShape collisionShape = blockState.getCollisionShape(level, fromHeight.above(y - fromHeight.getY()));
            if (collisionShape.isEmpty()) continue;
            double minY = collisionShape.min(Direction.Axis.Y);
            if (minY < from.y) continue;
            maxStepUp = Math.min(minY - playerHeight - from.y, maxStepUp);
            break;
        }

        List<PathSegment> segments = new ArrayList<>();
        moveableHeightAt(fromBlockPos.west(), from, maxStepUp + from.y)
                .ifPresent(h -> segments.add(new StraightPathSegment(from, new Vec3(Mth.floor(from.x) - .5, h, Mth.floor(from.z) + 0.5))));
        moveableHeightAt(fromBlockPos.east(), from, maxStepUp + from.y)
                .ifPresent(h -> segments.add(new StraightPathSegment(from, new Vec3(Mth.floor(from.x) + 1.5, h, Mth.floor(from.z) + 0.5))));
        moveableHeightAt(fromBlockPos.north(), from, maxStepUp + from.y)
                .ifPresent(h -> segments.add(new StraightPathSegment(from, new Vec3(Mth.floor(from.x) + 0.5, h, Mth.floor(from.z) - .5))));
        moveableHeightAt(fromBlockPos.south(), from, maxStepUp + from.y)
                .ifPresent(h -> segments.add(new StraightPathSegment(from, new Vec3(Mth.floor(from.x) + 0.5, h, Mth.floor(from.z) + 1.5))));
        return segments;
    }

    private Optional<Double> moveableHeightAt(BlockPos start, Vec3 from, double maxFloorHeight) {
        Optional<Double> dropHeight = findDropHeight(start, from);
        if (dropHeight.isPresent()) {
            return dropHeight;
        }
        return findStepUpHeight(start, from.y, maxFloorHeight);
    }

    private Optional<Double> findStepUpHeight(BlockPos start, double startY, double maxFloorHeight) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return Optional.empty();

        int fromBlockY = Mth.floor(startY);
        int maxBlockHeight = Mth.floor(maxFloorHeight);
        for (int y = fromBlockY; y <= maxBlockHeight; y++) {
            BlockPos pos = start.above(y - fromBlockY);
            BlockState blockState = level.getBlockState(pos);
            VoxelShape collisionShape = blockState.getCollisionShape(level, pos);
            if (collisionShape.isEmpty()) {
                //we have air but the previous height was not walkable
                return Optional.empty();
            }
            Optional<Double> walkableHeight = PathingUtils.getWalkableHeight(pos);
            if (walkableHeight.isEmpty()) {
                continue;
            }
            return walkableHeight.get() > maxFloorHeight ? Optional.empty() : walkableHeight;
        }
        return Optional.empty();

    }

    private Optional<Double> findDropHeight(BlockPos start, Vec3 from) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return Optional.empty();
        }
        int minFloorHeight = Mth.floor(from.y - maxDropDistance);
        //first check if there is enough head  roon to walk over
        LocalPlayer player = PlayerUtils.getPlayer();
        BlockPos fromPos = new BlockPos(Mth.floor(from.x), Mth.floor(from.y), Mth.floor(from.z));
        BlockPos direction = start.subtract(fromPos);
        AABB aabb = player.getDimensions(player.getPose())
                .makeBoundingBox(Mth.floor(from.x) + .5, Mth.floor(from.y) + .5, Mth.floor(from.z) + .5)
                .expandTowards(direction.getX(), direction.getY(), direction.getZ());
        if (!level.noCollision(aabb)) {
            return Optional.empty();
        }

        for (int y = 0; y >= minFloorHeight; y--) {
            BlockPos pos = start.above(y);
            Optional<Double> walkableHeight = PathingUtils.getWalkableHeight(pos);
            if (walkableHeight.isEmpty()) {
                continue;
            }
            double floorHeight = walkableHeight.get();
            if (floorHeight < from.y - maxDropDistance) {
                return Optional.empty();
            }

            return Optional.of(floorHeight);
        }
        return Optional.empty();
    }


}
