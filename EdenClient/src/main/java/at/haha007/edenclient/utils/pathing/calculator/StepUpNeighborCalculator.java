package at.haha007.edenclient.utils.pathing.calculator;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CubeVoxelShape;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class StepUpNeighborCalculator implements NeighborCandidateCalculator {
    private final int maxStepUpDistance;

    public StepUpNeighborCalculator(int maxStepUpDistance) {
        this.maxStepUpDistance = maxStepUpDistance;
    }

    @Override
    public Collection<BlockPos> getValidNeighbors(BlockPos pos) {
        List<BlockPos> validNeighbors = new ArrayList<>();
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return validNeighbors;
        }
        int realMaxStepUpDistance = 0;
        for (int stepDistance = 0; stepDistance < maxStepUpDistance; stepDistance++) {
            BlockPos checkPos = pos.above(stepDistance + 2);
            BlockState state = level.getBlockState(checkPos);
            VoxelShape collisionShape = state.getCollisionShape(level, checkPos);
            if (!collisionShape.isEmpty() || !state.getFluidState().isEmpty()) {
                continue;
            }
            realMaxStepUpDistance = stepDistance + 1;
        }
        realMaxStepUpDistance = Math.min(realMaxStepUpDistance, maxStepUpDistance);
        BlockPos north = findWalkableBlock(level, pos.north(), realMaxStepUpDistance);
        BlockPos east = findWalkableBlock(level, pos.east(), realMaxStepUpDistance);
        BlockPos south = findWalkableBlock(level, pos.south(), realMaxStepUpDistance);
        BlockPos west = findWalkableBlock(level, pos.west(), realMaxStepUpDistance);
        if (north != null) validNeighbors.add(north);
        if (east != null) validNeighbors.add(east);
        if (south != null) validNeighbors.add(south);
        if (west != null) validNeighbors.add(west);

//        // Check positions up to maxStepUpDistance blocks above
//        for (int stepDistance = 1; stepDistance <= maxStepUpDistance; stepDistance++) {
//            // Check starting space once per step distance
//            if (!hasEnoughStartingSpace(pos, stepDistance)) {
//                break;
//            }
//
//            BlockPos above = pos.above(stepDistance);
//
//            // Cardinal directions only (no diagonals)
//            if (canStepUpTo(above.north(), stepDistance)) {
//                validNeighbors.add(above.north());
//            }
//            if (canStepUpTo(above.east(), stepDistance)) {
//                validNeighbors.add(above.east());
//            }
//            if (canStepUpTo(above.west(), stepDistance)) {
//                validNeighbors.add(above.west());
//            }
//            if (canStepUpTo(above.south(), stepDistance)) {
//                validNeighbors.add(above.south());
//            }
//        }

        return validNeighbors;
    }

    private BlockPos findWalkableBlock(ClientLevel level, BlockPos pos, int maxStepUpDistance) {
        BlockState state = level.getBlockState(pos);
        VoxelShape collisionShape = state.getCollisionShape(level, pos);
        if (!isFullBlock(collisionShape)) {
            return null;
        }
        for (int y = 1; y <= maxStepUpDistance; y++) {
            BlockPos check = pos.above(y);
            state = level.getBlockState(check);
            collisionShape = state.getCollisionShape(level, check);
            if (isFullBlock(collisionShape)) {
                continue;
            }
            //needs 2 empty collision shapes above
            BlockPos aboveA = check.above(1);
            BlockPos aboveB = check.above(2);
            if (!level.getBlockState(aboveA).getCollisionShape(level, aboveA).isEmpty()) {
                return null;
            }
            if (!level.getBlockState(aboveB).getCollisionShape(level, aboveB).isEmpty()) {
                return null;
            }
            return check;
        }
        return null;
    }

    private boolean isFullBlock(VoxelShape collisionShape) {
        AABB bounds = collisionShape.isEmpty() ? AABB.ofSize(Vec3.ZERO, 0, 0, 0) : collisionShape.bounds();
        Vec3 size = bounds.getMaxPosition().subtract(bounds.getMinPosition());
        return collisionShape instanceof CubeVoxelShape && size.x() > 0.99 && size.y() > 0.99 && size.z() > 0.99;
    }
}
