package at.haha007.edenclient.utils.pathing.calculator;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

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

        // Check positions up to maxStepUpDistance blocks above
        for (int stepDistance = 1; stepDistance <= maxStepUpDistance; stepDistance++) {
            // Check starting space once per step distance
            if (!hasEnoughStartingSpace(pos, stepDistance)) {
                break;
            }

            BlockPos above = pos.above(stepDistance);

            // Cardinal directions only (no diagonals)
            if (canStepUpTo(above.north(), stepDistance)) {
                validNeighbors.add(above.north());
            }
            if (canStepUpTo(above.east(), stepDistance)) {
                validNeighbors.add(above.east());
            }
            if (canStepUpTo(above.west(), stepDistance)) {
                validNeighbors.add(above.west());
            }
            if (canStepUpTo(above.south(), stepDistance)) {
                validNeighbors.add(above.south());
            }
        }

        return validNeighbors;
    }

    private boolean hasEnoughStartingSpace(BlockPos startPos, int stepDistance) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return false;
        }

        // Check there's enough space above the starting position to step up
        // Need clear space from startPos to startPos.above(stepDistance + 1)
        for (int i = 0; i <= stepDistance + 1; i++) {
            BlockPos b = startPos.above(i);
            if (!level.getBlockState(b).getCollisionShape(level, b).isEmpty()) {
                return  false;
            }
        }
        return  true;
    }

    private boolean canStepUpTo(BlockPos targetPos, int stepDistance) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return false;
        }

        // Check target position has solid blocks below
        BlockState targetFloor = level.getBlockState(targetPos.below());
        if (!targetFloor.isFaceSturdy(level, targetPos.below(), Direction.UP)) {
            return false;
        }

        // Check all blocks below target are solid (from target.below() down to startPos.below())
        for (int y = 1; y <= stepDistance; y++) {
            BlockPos checkPos = targetPos.below(stepDistance);
            BlockState blockState = level.getBlockState(checkPos);
            if (!blockState.isCollisionShapeFullBlock(level, checkPos)) {
                return false;
            }
        }

        // Check target position itself is walkable (clear space above)
        AABB targetSpace = AABB.encapsulatingFullBlocks(targetPos, targetPos.above());
        return level.noCollision(targetSpace);
    }
}
