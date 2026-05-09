package at.haha007.edenclient.utils.pathing.segmentcalculator;

import at.haha007.edenclient.utils.PlayerUtils;
import at.haha007.edenclient.utils.pathing.PathingUtils;
import at.haha007.edenclient.utils.pathing.segment.JumpUpPathSegment;
import at.haha007.edenclient.utils.pathing.segment.PathSegment;
import at.haha007.edenclient.utils.pathing.segment.StraightPathSegment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.*;

public class StraightSegmentCalculator implements SegmentCalculator {
    private static final int COLLISION_CACHE_SIZE = 4_096;
    private static final int MAX_COLLISION_SOURCE_BLOCK_EXTENSION_BELOW = 1;

    private final double maxDropDistance;
    private final Map<Long, List<AABB>> collisionBoxesCache = new LinkedHashMap<>(256, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Long, List<AABB>> eldest) {
            return size() > COLLISION_CACHE_SIZE;
        }
    };

    private final Set<Long> fluidBlocks = new HashSet<>();
    private ClientLevel cachedCollisionLevel;
    private long cachedCollisionGameTime = Long.MIN_VALUE;

    public StraightSegmentCalculator(double maxDropDistance) {
        this.maxDropDistance = maxDropDistance;
    }

    @Override
    public Collection<PathSegment> calculateSegments(Vec3 from) {
        LocalPlayer player = PlayerUtils.getPlayer();
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return Collections.emptyList();
        resetCollisionCacheIfNeeded(level);
        float playerWidth = player.getBbWidth();
        List<PathSegment> segments = new ArrayList<>();
        float maxStepUp = PlayerUtils.getPlayer().maxUpStep();
        List<Vec3> reachableBlockOffsets = getReachableBlocks(from, playerWidth, maxStepUp);
        for (Vec3 target : reachableBlockOffsets) {
            double deltaY = target.y() - from.y();
            if (deltaY > maxStepUp) {
                segments.add(new JumpUpPathSegment(from, target));
            } else {
                segments.add(new StraightPathSegment(from, target, 1));
            }
        }
        return segments;
    }

    private List<Vec3> getReachableBlocks(Vec3 from, float playerWidth, double maxStepUp) {
        LocalPlayer player = PlayerUtils.getPlayer();
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return List.of();
        }

        int minX = Mth.floor(from.x - playerWidth);
        int maxX = Mth.floor(from.x + playerWidth);
        int minZ = Mth.floor(from.z - playerWidth);
        int maxZ = Mth.floor(from.z + playerWidth);
        double jumpHeight = PathingUtils.getMaxJumpHeight(PathingUtils.getJumpPower());
        if (canUseJumpHeight(from, player, level)) {
            maxStepUp = Math.max(jumpHeight, maxStepUp);
        }
        List<Vec3> reachableBlockOffsets = new ArrayList<>();
        List<Vec3> blockOffsets = getAllBlockOffsets(playerWidth);
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (Vec3 blockOffset : blockOffsets) {
                    Vec3 to = blockOffset.add(x, 0, z);
                    double deltaX = Math.abs(from.x - to.x);
                    double deltaZ = Math.abs(from.z - to.z);
                    if (deltaX > playerWidth || deltaZ > playerWidth) {
                        continue;
                    }
                    double targetY = getTargetY(from, to, maxStepUp);
                    if (Double.isNaN(targetY)) {
                        continue;
                    }
                    reachableBlockOffsets.add(to.add(0, targetY - to.y, 0));
                }
            }
        }
        return reachableBlockOffsets;
    }

    private boolean canUseJumpHeight(Vec3 from, LocalPlayer player, ClientLevel level) {
        EntityDimensions dimensions = player.getDimensions(player.getPose());
        double halfWidth = dimensions.width() / 2;
        double playerHeight = dimensions.height();
        AABB startBox = new AABB(
                from.x - halfWidth, from.y, from.z - halfWidth,
                from.x + halfWidth, from.y + playerHeight, from.z + halfWidth
        );
        CollisionContext collisionContext = CollisionContext.of(player);

        if (hasBlockCollision(level, startBox, collisionContext)) {
            return false;
        }
        if (!hasBlockCollision(level, startBox.move(0, -0.05, 0), collisionContext)) {
            return false;
        }

        int minBlockX = Mth.floor(startBox.minX + 1e-7);
        int maxBlockX = Mth.floor(startBox.maxX - 1e-7);
        int minBlockY = Mth.floor(startBox.minY + 1e-7);
        int maxBlockY = Mth.floor(startBox.maxY - 1e-7);
        int minBlockZ = Mth.floor(startBox.minZ + 1e-7);
        int maxBlockZ = Mth.floor(startBox.maxZ - 1e-7);

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = minBlockX; x <= maxBlockX; x++) {
            for (int y = minBlockY; y <= maxBlockY; y++) {
                for (int z = minBlockZ; z <= maxBlockZ; z++) {
                    pos.set(x, y, z);
                    if (level.getBlockState(pos).is(Blocks.COBWEB)
                            || level.getBlockState(pos).is(BlockTags.CLIMBABLE)
                            || level.getBlockState(pos).is(Blocks.POWDER_SNOW)) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    //first calculates if it has to step up/fall down
    //returns the target y if the target x/z is reachable and the fall distance is not too high
    //returns NaN if not reachable
    private double getTargetY(Vec3 from, Vec3 to, double maxStepUp) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return Double.NaN;
        }

        LocalPlayer player = PlayerUtils.getPlayer();
        EntityDimensions dimensions = player.getDimensions(player.getPose());
        double halfWidth = dimensions.width() / 2;
        double playerHeight = dimensions.height();
        double minTargetY = from.y - maxDropDistance;
        double maxTargetY = from.y + maxStepUp;

        double minX = to.x - halfWidth;
        double maxX = to.x + halfWidth;
        double minZ = to.z - halfWidth;
        double maxZ = to.z + halfWidth;
        int minBlockX = (int) Math.floor(minX + 1e-6);
        int maxBlockX = (int) Math.floor(maxX - 1e-6);
        int minBlockZ = (int) Math.floor(minZ + 1e-6);
        int maxBlockZ = (int) Math.floor(maxZ - 1e-6);
        int minBlockY = (int) Math.floor(minTargetY) - 1;
        int maxBlockY = (int) Math.floor(maxTargetY);

        double moveX = to.x - from.x;
        double moveZ = to.z - from.z;
        AABB startBox = new AABB(
                from.x - halfWidth, from.y, from.z - halfWidth,
                from.x + halfWidth, from.y + playerHeight, from.z + halfWidth
        );
        AABB horizontalEndAtStartY = startBox.move(moveX, 0, moveZ);

        List<Double> candidateYs = new ArrayList<>(16);
        Set<Long> seenHeights = new HashSet<>(32);
        CollisionContext collisionContext = CollisionContext.of(player);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = minBlockX; x <= maxBlockX; x++) {
            for (int z = minBlockZ; z <= maxBlockZ; z++) {
                for (int y = minBlockY; y <= maxBlockY; y++) {
                    pos.set(x, y, z);
                    for (AABB worldBox : getCollisionBoxes(level, pos, collisionContext)) {
                        if (worldBox.maxY < minTargetY - 1e-4 || worldBox.maxY > maxTargetY + 1e-4) {
                            continue;
                        }
                        if (worldBox.maxX <= minX || worldBox.minX >= maxX || worldBox.maxZ <= minZ || worldBox.minZ >= maxZ) {
                            continue;
                        }

                        long key = Math.round(worldBox.maxY * 1000);
                        if (seenHeights.add(key)) {
                            candidateYs.add(worldBox.maxY);
                        }
                    }
                }
            }
        }

        candidateYs.sort(Comparator.<Double>comparingDouble(y -> Math.abs(y - from.y)).thenComparing(Comparator.reverseOrder()));
        for (double candidateY : candidateYs) {
            double deltaY = candidateY - from.y;
            if (deltaY > maxStepUp + 1e-4 || deltaY < -maxDropDistance - 1e-4) {
                continue;
            }

            AABB targetBox = new AABB(
                    to.x - halfWidth, candidateY, to.z - halfWidth,
                    to.x + halfWidth, candidateY + playerHeight, to.z + halfWidth
            );
            if (hasFluidCollision(level, targetBox)) {
                continue;
            }
            if (hasBlockCollision(level, targetBox, collisionContext)) {
                continue;
            }
            if (!hasBlockCollision(level, targetBox.move(0, -0.05, 0), collisionContext)) {
                continue;
            }
            if (hasBlockCollision(level, targetBox.move(from.subtract(to).multiply(.5, 0, .5)), collisionContext)
                    && hasBlockCollision(level, startBox.move(to.subtract(from).multiply(.5, 0, .5)), collisionContext)) {
                continue;
            }

            boolean reachable;
            if (deltaY > 1e-4) {
                reachable = PathingUtils.isStraightCollisionFreeMove(startBox, Direction.UP, deltaY);
            } else if (deltaY < -1e-4) {
                reachable = PathingUtils.isStraightCollisionFreeMove(horizontalEndAtStartY, Direction.DOWN, -deltaY);
            } else {
                reachable = true;
            }
            if (reachable) {
                return candidateY;
            }
        }

        return Double.NaN;
    }

    private boolean hasFluidCollision(ClientLevel level, AABB box) {
        int minBlockX = Mth.floor(box.minX + 1e-7);
        int maxBlockX = Mth.floor(box.maxX - 1e-7);
        int minBlockY = Mth.floor(box.minY + 1e-7);
        int maxBlockY = Mth.floor(box.maxY - 1e-7);
        int minBlockZ = Mth.floor(box.minZ + 1e-7);
        int maxBlockZ = Mth.floor(box.maxZ - 1e-7);

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = minBlockX; x <= maxBlockX; x++) {
            for (int y = minBlockY; y <= maxBlockY; y++) {
                for (int z = minBlockZ; z <= maxBlockZ; z++) {
                    pos.set(x, y, z);
                    long key = pos.asLong();
                    if (fluidBlocks.contains(key)) {
                        return true;
                    }
                    if (level.getFluidState(pos).isEmpty()) {
                        continue;
                    }
                    fluidBlocks.add(key);
                    return true;
                }
            }
        }

        return false;
    }

    private void resetCollisionCacheIfNeeded(ClientLevel level) {
        long gameTime = level.getGameTime();
        if (level == cachedCollisionLevel && gameTime == cachedCollisionGameTime) {
            return;
        }
        collisionBoxesCache.clear();
        fluidBlocks.clear();
        cachedCollisionLevel = level;
        cachedCollisionGameTime = gameTime;
    }

    private boolean hasBlockCollision(ClientLevel level, AABB box, CollisionContext collisionContext) {
        int minBlockX = Mth.floor(box.minX + 1e-7);
        int maxBlockX = Mth.floor(box.maxX - 1e-7);
        int minBlockY = Mth.floor(box.minY + 1e-7) - MAX_COLLISION_SOURCE_BLOCK_EXTENSION_BELOW;
        int maxBlockY = Mth.floor(box.maxY - 1e-7);
        int minBlockZ = Mth.floor(box.minZ + 1e-7);
        int maxBlockZ = Mth.floor(box.maxZ - 1e-7);

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = minBlockX; x <= maxBlockX; x++) {
            for (int y = minBlockY; y <= maxBlockY; y++) {
                for (int z = minBlockZ; z <= maxBlockZ; z++) {
                    pos.set(x, y, z);
                    for (AABB collisionBox : getCollisionBoxes(level, pos, collisionContext)) {
                        if (collisionBox.intersects(box)) {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    private List<AABB> getCollisionBoxes(ClientLevel level, BlockPos pos, CollisionContext collisionContext) {
        long key = pos.asLong();
        List<AABB> cachedBoxes = collisionBoxesCache.get(key);
        if (cachedBoxes != null) {
            return cachedBoxes;
        }

        VoxelShape collisionShape = level.getBlockState(pos).getCollisionShape(level, pos, collisionContext);
        List<AABB> localBoxes = collisionShape.toAabbs();
        if (localBoxes.isEmpty()) {
            collisionBoxesCache.put(key, List.of());
            return List.of();
        }

        List<AABB> worldBoxes = new ArrayList<>(localBoxes.size());
        for (AABB localBox : localBoxes) {
            worldBoxes.add(localBox.move(pos));
        }

        List<AABB> cachedWorldBoxes = List.copyOf(worldBoxes);
        collisionBoxesCache.put(key, cachedWorldBoxes);
        return cachedWorldBoxes;
    }


    private List<Vec3> getAllBlockOffsets(float playerWidth) {

        List<Vec3> blockOffsets = new ArrayList<>();
        // hugging walls
        float halfPlayerWidth = playerWidth / 2;
        blockOffsets.add(new Vec3(halfPlayerWidth, 0, 0));
        blockOffsets.add(new Vec3(1 - halfPlayerWidth, 0, 0));
        blockOffsets.add(new Vec3(0, 0, halfPlayerWidth));
        blockOffsets.add(new Vec3(0, 0, 1 - halfPlayerWidth));
        blockOffsets.add(new Vec3(1 - halfPlayerWidth, 0, 1 - halfPlayerWidth));
        blockOffsets.add(new Vec3(halfPlayerWidth, 0, 1 - halfPlayerWidth));
        blockOffsets.add(new Vec3(1 - halfPlayerWidth, 0, halfPlayerWidth));
        blockOffsets.add(new Vec3(halfPlayerWidth, 0, halfPlayerWidth));
        // middle of block
        // between blocks
        blockOffsets.add(new Vec3(0, 0, 0));
        blockOffsets.add(new Vec3(0, 0, .5));
        blockOffsets.add(new Vec3(0, 0, 1));
        blockOffsets.add(new Vec3(.5, 0, 0));
        blockOffsets.add(new Vec3(.5, 0, .5));
        blockOffsets.add(new Vec3(.5, 0, 1));
        blockOffsets.add(new Vec3(1, 0, 0));
        blockOffsets.add(new Vec3(1, 0, .5));
        blockOffsets.add(new Vec3(1, 0, 1));
        //next to fence
        double fenceOffset = .5 - halfPlayerWidth - 2 / 16d;
//        blockOffsets.add(new Vec3(fenceOffset, 0, 0));
//        blockOffsets.add(new Vec3(1 - fenceOffset, 0, 0));
//        blockOffsets.add(new Vec3(0, 0, fenceOffset));
//        blockOffsets.add(new Vec3(0, 0, 1 - fenceOffset));
        blockOffsets.add(new Vec3(1 - fenceOffset, 0, 1 - fenceOffset));
        blockOffsets.add(new Vec3(fenceOffset, 0, 1 - fenceOffset));
        blockOffsets.add(new Vec3(1 - fenceOffset, 0, fenceOffset));
        blockOffsets.add(new Vec3(fenceOffset, 0, fenceOffset));
        blockOffsets.add(new Vec3(fenceOffset, 0, .5));
        blockOffsets.add(new Vec3(1 - fenceOffset, 0, .5));
        blockOffsets.add(new Vec3(.5, 0, fenceOffset));
        blockOffsets.add(new Vec3(.5, 0, 1 - fenceOffset));


        return blockOffsets;
    }
}
