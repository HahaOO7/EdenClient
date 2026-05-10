package at.haha007.edenclient.utils.pathing.segmentcalculator;

import at.haha007.edenclient.utils.PlayerUtils;
import at.haha007.edenclient.utils.pathing.segment.PathSegment;
import at.haha007.edenclient.utils.pathing.segment.StraightPathSegment;
import at.haha007.edenclient.utils.pathing.segment.SwimmingPathSegment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.*;

public class SwimmingSegmentCalculator implements SegmentCalculator {
    private record BlockData(boolean solid, boolean water, boolean collisionFree) {
    }

    private final Map<Long, BlockData> blockCache = new HashMap<>();
    private long cachedCollisionGameTime;
    private ClientLevel cachedCollisionLevel;


    @Override
    public Collection<PathSegment> calculateSegments(Vec3 from) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return List.of();
        }
        resetCollisionCacheIfNeeded(level);
        AABB startingAABB = PlayerUtils.getPlayer().getDimensions(Pose.STANDING).makeBoundingBox(from);
        if (isOnOneBlock(startingAABB)) {
            return getTargetsOnOneBlock(level, from);
        } else {
            return getTargetsBetweenBlocks(level, startingAABB);
        }
    }

    private Collection<PathSegment> getTargetsBetweenBlocks(ClientLevel level, AABB fromAABB) {
        AABB bottomAABB = fromAABB.setMaxY(fromAABB.minY);
        List<BlockPos> onBlocks = getOnBlocks(bottomAABB);
        List<PathSegment> segments = new ArrayList<>();
        for (BlockPos block : onBlocks) {
            Vec3 toPos = Vec3.atBottomCenterOf(block);
            double cost = getMovementCost(block, level);
            if (Double.isNaN(cost)) {
                continue;
            }
            segments.add(new SwimmingPathSegment(bottomAABB.getCenter(), toPos, cost));
        }
        return segments;
    }

    private Collection<PathSegment> getTargetsOnOneBlock(ClientLevel level, Vec3 from) {
        BlockPos fromPos = BlockPos.containing(from);
        BlockData data = getBlockData(fromPos, level);
        if(data.water){
            //if in water step to center of neighbors
            return getStepFromWater(level, from);
        }else{
            //if outside water step in water
            return getStepIntoWater(level, from);
        }
    }

    private Collection<PathSegment> getStepFromWater(ClientLevel level, Vec3 from) {
        BlockPos fromPos = BlockPos.containing(from);
        List<PathSegment> segments = new ArrayList<>();
        //from water to water
        double cost = getMovementCost(fromPos.north(), level);
        if (!Double.isNaN(cost)) {
            segments.add(new SwimmingPathSegment(from, Vec3.atBottomCenterOf(fromPos.north()), cost));
        }
        cost = getMovementCost(fromPos.south(), level);
        if (!Double.isNaN(cost)) {
            segments.add(new SwimmingPathSegment(from, Vec3.atBottomCenterOf(fromPos.south()), cost));
        }
        cost = getMovementCost(fromPos.east(), level);
        if (!Double.isNaN(cost)) {
            segments.add(new SwimmingPathSegment(from, Vec3.atBottomCenterOf(fromPos.east()), cost));
        }
        cost = getMovementCost(fromPos.west(), level);
        if (!Double.isNaN(cost)) {
            segments.add(new SwimmingPathSegment(from, Vec3.atBottomCenterOf(fromPos.west()), cost));
        }
        cost = getMovementCost(fromPos.above(), level);
        if (!Double.isNaN(cost)) {
            segments.add(new SwimmingPathSegment(from, Vec3.atBottomCenterOf(fromPos.above()), cost));
        }
        cost = getMovementCost(fromPos.below(), level);
        if (!Double.isNaN(cost)) {
            segments.add(new SwimmingPathSegment(from, Vec3.atBottomCenterOf(fromPos.below()), cost));
        }
        //from water to land
        if(getBlockData(fromPos.above(), level).water){
            return segments;
        }
        if(canStepOut(fromPos.north(), level)){
            segments.add(new SwimmingPathSegment(from, Vec3.atBottomCenterOf(fromPos.north().above()), 10));
        }
        if(canStepOut(fromPos.south(), level)){
            segments.add(new SwimmingPathSegment(from, Vec3.atBottomCenterOf(fromPos.south().above()), 10));
        }
        if(canStepOut(fromPos.east(), level)){
            segments.add(new SwimmingPathSegment(from, Vec3.atBottomCenterOf(fromPos.east().above()), 10));
        }
        if(canStepOut(fromPos.west(), level)){
            segments.add(new SwimmingPathSegment(from, Vec3.atBottomCenterOf(fromPos.west().above()), 10));
        }

        return segments;
    }

    private boolean canStepOut(BlockPos pos, ClientLevel level) {
        BlockData data = getBlockData(pos, level);
        if(!data.solid){
            return false;
        }
        data = getBlockData(pos.above(), level);
        if(!data.collisionFree) {
            return false;
        }
        return getBlockData(pos.above(2), level).collisionFree;
    }

    private Collection<PathSegment> getStepIntoWater(ClientLevel level, Vec3 fromVec) {
        BlockPos fromPos = BlockPos.containing(fromVec);
        BlockData data = getBlockData(fromPos, level);
        if (data == null || !data.collisionFree) {
            return List.of();
        }
        data = getBlockData(fromPos.above(), level);
        if (data == null || !data.collisionFree) {
            return List.of();
        }
        List<PathSegment> segments = new ArrayList<>();

        //north
        BlockPos north = fromPos.north();
        data = getBlockData(north.above(), level);
        if (data.collisionFree) {
            double cost = getMovementCost(north.below(), level);
            if (!Double.isNaN(cost)) {
                segments.add(new StraightPathSegment(fromVec, Vec3.atBottomCenterOf(north.below()), cost));
            }
        }
        //south
        BlockPos south = fromPos.south();
        data = getBlockData(south.above(), level);
        if (data.collisionFree) {
            double cost = getMovementCost(south.below(), level);
            if (!Double.isNaN(cost)) {
                segments.add(new SwimmingPathSegment(fromVec, Vec3.atBottomCenterOf(south.below()), cost));
            }
        }
        //west
        BlockPos west = fromPos.west();
        data = getBlockData(west.above(), level);
        if (data.collisionFree) {
            double cost = getMovementCost(west.below(), level);
            if (!Double.isNaN(cost)) {
                segments.add(new SwimmingPathSegment(fromVec, Vec3.atBottomCenterOf(west.below()), cost));
            }
        }
        //east
        BlockPos east = fromPos.east();
        data = getBlockData(east.above(), level);
        if (data.collisionFree) {
            double cost = getMovementCost(east.below(), level);
            if (!Double.isNaN(cost)) {
                segments.add(new SwimmingPathSegment(fromVec, Vec3.atBottomCenterOf(east.below()), cost));
            }
        }

        return segments;
    }

    private boolean isOnOneBlock(AABB aabb) {
        return Mth.floor(aabb.minX) == Mth.floor(aabb.maxX)
                && Mth.floor(aabb.minZ) == Mth.floor(aabb.maxZ);
    }

    private List<BlockPos> getOnBlocks(AABB boundingBox) {
        List<BlockPos> standingOnBlocks = new ArrayList<>();
        int minX = Mth.floor(boundingBox.minX);
        int maxX = Mth.floor(boundingBox.maxX);
        int minZ = Mth.floor(boundingBox.minZ);
        int maxZ = Mth.floor(boundingBox.maxZ);
        int y = Mth.floor(boundingBox.minY);
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                standingOnBlocks.add(new BlockPos(x, y, z));
            }
        }
        return standingOnBlocks;
    }


    private double getMovementCost(BlockPos pos, ClientLevel level) {
        BlockData blockData = getBlockData(pos, level);
        if (blockData == null) {
            return Double.NaN;
        }
        if (!blockData.water) {
            return Double.NaN;
        }
        if (!blockData.collisionFree) {
            return Double.NaN;
        }

        BlockData blockDataAbove = getBlockData(pos.above(), level);
        if (blockDataAbove == null) {
            return Double.NaN;
        }
        if (!blockDataAbove.collisionFree) {
            return Double.NaN;
        }
        //prefer the head sticking out of the water
        return blockDataAbove.water ? 10 : 5;
    }


    private BlockData getBlockData(BlockPos pos, ClientLevel level) {
        return blockCache.computeIfAbsent(pos.asLong(), key -> {
            boolean solid = level.getBlockState(pos).isCollisionShapeFullBlock(level, pos);
            boolean water = level.getFluidState(pos).is(Fluids.WATER) && level.getFluidState(pos).isSource();
            boolean collisionFree = level.getBlockState(pos).getCollisionShape(level, pos).isEmpty();
            return new BlockData(solid, water, collisionFree);
        });
    }

    private void resetCollisionCacheIfNeeded(ClientLevel level) {
        long gameTime = level.getGameTime();
        if (level == cachedCollisionLevel && gameTime == cachedCollisionGameTime) {
            return;
        }
        blockCache.clear();
        cachedCollisionLevel = level;
        cachedCollisionGameTime = gameTime;
    }

}
