package at.haha007.edenclient.utils.pathing;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SimplePathFinder {

    public static Path findPath(Vec3i start, Vec3i end) {
        if (start.getY() != end.getY()) {
            return null;
        }
        BlockPos startingBlock = new BlockPos(start);
        BlockPos endingBlock = new BlockPos(end);

        List<BlockPos> openList = new ArrayList<>();
        List<BlockPos> closedList = new ArrayList<>();
        Map<BlockPos, BlockPos> parentMap = new HashMap<>();
        openList.add(startingBlock);

        while (!openList.isEmpty()) {
            BlockPos current = openList.get(0);
            openList.remove(0);
            closedList.add(current);

            if (current.equals(endingBlock)) {
                return reconstructPath(parentMap, startingBlock, endingBlock);
            }

            for (Direction direction : Direction.values()) {
                BlockPos neighbor = current.relative(direction);
                if (!closedList.contains(neighbor) && !openList.contains(neighbor) && isWalkable(neighbor)) {
                    openList.add(neighbor);
                    parentMap.put(neighbor, current);
                }
            }
        }
        return null;
    }

    private static Path reconstructPath(Map<BlockPos, BlockPos> parentMap, BlockPos start, BlockPos end) {
        List<BlockPos> path = new ArrayList<>();
        BlockPos current = end;
        
        while (current != null && !current.equals(start)) {
            path.add(current);
            current = parentMap.get(current);
        }
        
        if (current != null) {
            path.add(start);
        }
        
        Collections.reverse(path);
        return new Path(path);
    }


    private static boolean isWalkable(BlockPos pos) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return false;
        }
        //floor has to have a solid top surface and 2 blocks without collision above
        BlockState floorState = level.getBlockState(pos.below());
        if (!floorState.isFaceSturdy(level, pos.below(), Direction.UP)) {
            return false;
        }
        return level.noCollision(new AABB(Vec3.atLowerCornerOf(pos), Vec3.atLowerCornerOf(pos).add(1, 2, 1)));
    }
}
