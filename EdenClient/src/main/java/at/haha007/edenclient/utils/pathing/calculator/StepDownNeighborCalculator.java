package at.haha007.edenclient.utils.pathing.calculator;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class StepDownNeighborCalculator implements NeighborCandidateCalculator {
    private static final byte SOLID = 1;
    private static final byte NO_COLLISION = 2;
    private static final byte OTHER = 3;

    private final int maxDropDistance;

    public StepDownNeighborCalculator(int maxDropDistance) {
        this.maxDropDistance = maxDropDistance;
    }

    @Override
    public Collection<BlockPos> getValidNeighbors(BlockPos pos) {
        List<BlockPos> validNeighbors = new ArrayList<>();
        byte[] solidStates = getAllSolidStates(pos);
        if (solidStates.length == 0) return validNeighbors;

        int west = findWalkableBlock(-1, 0, solidStates);
        int east = findWalkableBlock(1, 0, solidStates);
        int north = findWalkableBlock(0, -1, solidStates);
        int south = findWalkableBlock(0, 1, solidStates);
        int northEast = findWalkableBlock(1, -1, solidStates);
        int northWest = findWalkableBlock(-1, -1, solidStates);
        int southEast = findWalkableBlock(1, 1, solidStates);
        int southWest = findWalkableBlock(-1, 1, solidStates);

        if (west <= 0) validNeighbors.add(pos.offset(-1, west, 0));
        if (east <= 0) validNeighbors.add(pos.offset(1, east, 0));
        if (north <= 0) validNeighbors.add(pos.offset(0, north, -1));
        if (south <= 0) validNeighbors.add(pos.offset(0, south, 1));

        if (northEast <= 0 && east <= 0 && north <= 0) validNeighbors.add(pos.offset(1, northEast, -1));
        if (northWest <= 0 && west <= 0 && north <= 0) validNeighbors.add(pos.offset(-1, northWest, -1));
        if (southEast <= 0 && east <= 0 && south <= 0) validNeighbors.add(pos.offset(1, southEast, 1));
        if (southWest <= 0 && west <= 0 && south <= 0) validNeighbors.add(pos.offset(-1, southWest, 1));

        return validNeighbors;
    }

    private byte[] getAllSolidStates(BlockPos center) {
        //y = maxDropDistance + 1 (floor) + 2 (player)
        byte[] solidStates = new byte[(maxDropDistance + 3) * 3 * 3];
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return new byte[0];
        }
        for (int offsetY = -maxDropDistance - 1; offsetY < 2; offsetY++) {
            for (int offsetX = -1; offsetX < 2; offsetX++) {
                for (int offsetZ = -1; offsetZ < 2; offsetZ++) {
                    Vec3i offset = new Vec3i(offsetX, offsetY, offsetZ);
                    int index = toIndex(offset);
                    BlockPos globalPos = center.offset(offset);
                    BlockState state = level.getBlockState(globalPos);
                    byte shape;
                    VoxelShape collisionShape = state.getCollisionShape(level, globalPos);
                    AABB bounds = collisionShape.isEmpty() ? AABB.ofSize(Vec3.ZERO, 0, 0, 0) : collisionShape.bounds();
                    Vec3 size = bounds.getMaxPosition().subtract(bounds.getMinPosition());
                    boolean notFluid = state.getFluidState().isEmpty();
                    if (size.x() >= 0.99 && size.y() > 0.99 && size.z() > 0.99) {
                        shape = SOLID;
                    } else if (collisionShape.isEmpty() && notFluid) {
                        shape = NO_COLLISION;
                    } else {
                        shape = OTHER;
                    }
                    solidStates[index] = shape;
                }
            }
        }

        return solidStates;
    }

    //finds the highest walkable block for the given offset.
    //returns 0 if no walkable block is found
    private int findWalkableBlock(int x, int z, byte[] states) {
        //to drop down at least 3 blocks of air are needed
        for (int y = 1; y >= -maxDropDistance; y--) {
            int index = toIndex(x, y, z);
            if (states[index] == SOLID)
                return y + 1;
            if (states[index] == OTHER)
                return 2;
        }
        return 2;
    }

    private int toIndex(Vec3i offset) {
        return toIndex(offset.getX(), offset.getY(), offset.getZ());
    }

    private int toIndex(int x, int y, int z) {
        //x range: -1 to 1
        //y range: -maxDropDistance - 1 to 1
        //z range: -1 to 1
        return (x + 1)
                + (y + maxDropDistance + 1) * 3
                + (z + 1) * 3 * (maxDropDistance + 2);
    }
}
