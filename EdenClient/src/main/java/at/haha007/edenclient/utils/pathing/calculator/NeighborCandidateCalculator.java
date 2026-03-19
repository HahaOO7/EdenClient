package at.haha007.edenclient.utils.pathing.calculator;

import net.minecraft.core.BlockPos;

import java.util.Collection;

public interface NeighborCandidateCalculator {
    Collection<BlockPos> getValidNeighbors(BlockPos pos);
}
