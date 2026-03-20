package at.haha007.edenclient.utils.pathing.calculator;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class StepDownNeighborCalculator implements NeighborCandidateCalculator {
    private final int maxDropDistance;

    public StepDownNeighborCalculator(int maxDropDistance) {
        this.maxDropDistance = maxDropDistance;
    }

    @Override
    public Collection<BlockPos> getValidNeighbors(BlockPos pos) {
        List<BlockPos> validNeighbors = new ArrayList<>();

        BlockPos west = findWalkableBlock(pos.west());
        BlockPos east = findWalkableBlock(pos.east());
        BlockPos north = findWalkableBlock(pos.north());
        BlockPos south = findWalkableBlock(pos.south());

        if (west != null) validNeighbors.add(west);
        if (east != null) validNeighbors.add(east);
        if (north != null) validNeighbors.add(north);
        if (south != null) validNeighbors.add(south);

        return validNeighbors;
    }

    private BlockPos findWalkableBlock(BlockPos pos) {
        //to drop down at least 3 blocks of air are needed
        pos = pos.above();
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return null;
        }
        for (int y = -2; y <= maxDropDistance; y++) {
            BlockState state = level.getBlockState(pos);
            VoxelShape collisionShape = state.getCollisionShape(level, pos);
            AABB bounds = collisionShape.isEmpty() ? AABB.ofSize(Vec3.ZERO, 0, 0, 0) : collisionShape.bounds();
            Vec3 size = bounds.getMaxPosition().subtract(bounds.getMinPosition());
            boolean notFluid = state.getFluidState().isEmpty();
            if (size.x() > 0.99 && size.y() > 0.99 && size.z() > 0.99) {
                return y >= 0 ? pos.above() : null;
            } else if (!collisionShape.isEmpty() || !notFluid) {
                return null;
            }
            pos = pos.below();
        }
        return null;
    }
}
