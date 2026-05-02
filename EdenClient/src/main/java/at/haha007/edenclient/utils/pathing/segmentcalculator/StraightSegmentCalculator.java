package at.haha007.edenclient.utils.pathing.segmentcalculator;

import at.haha007.edenclient.utils.PlayerUtils;
import at.haha007.edenclient.utils.pathing.PathingUtils;
import at.haha007.edenclient.utils.pathing.segment.JumpUpPathSegment;
import at.haha007.edenclient.utils.pathing.segment.PathSegment;
import at.haha007.edenclient.utils.pathing.segment.StraightPathSegment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class StraightSegmentCalculator implements SegmentCalculator {
    private final double maxDropDistance;

    public StraightSegmentCalculator(double maxDropDistance) {
        this.maxDropDistance = maxDropDistance;
    }

    @Override
    public Collection<PathSegment> calculateSegments(Vec3 from) {
        LocalPlayer player = PlayerUtils.getPlayer();
        float playerWidth = player.getBbWidth();
        List<PathSegment> segments = new ArrayList<>();
        float maxStepUp = PlayerUtils.getPlayer().maxUpStep();
        List<Vec3> reachableBlockOffsets = getReachableBlocks(from, playerWidth, maxStepUp);
        for (Vec3 target : reachableBlockOffsets) {
            double deltaY = target.y() - from.y();
            if(deltaY > maxStepUp) {
                segments.add(new JumpUpPathSegment(from, target));
            }else {
                segments.add(new StraightPathSegment(from, target));
            }
        }
        return segments;
    }

    private List<Vec3> getReachableBlocks(Vec3 from, float playerWidth, double maxStepUp) {
        int minX = Mth.floor(from.x - playerWidth);
        int maxX = Mth.floor(from.x + playerWidth);
        int minZ = Mth.floor(from.z - playerWidth);
        int maxZ = Mth.floor(from.z + playerWidth);
        maxStepUp = Math.max(PathingUtils.getMaxJumpHeight(PathingUtils.getJumpPower()), maxStepUp);
        System.out.println(maxStepUp);
        List<Vec3> reachableBlockOffsets = new ArrayList<>();
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                List<Vec3> blockOffsets = getAllBlockOffsets(playerWidth, x, z);
                for (Vec3 to : blockOffsets) {
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

        List<Double> candidateYs = new ArrayList<>();
        Set<Long> seenHeights = new HashSet<>();
        CollisionContext collisionContext = CollisionContext.of(player);
        for (int x = minBlockX; x <= maxBlockX; x++) {
            for (int z = minBlockZ; z <= maxBlockZ; z++) {
                for (int y = minBlockY; y <= maxBlockY; y++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    VoxelShape collisionShape = level.getBlockState(pos).getCollisionShape(level, pos, collisionContext);
                    for (AABB localBox : collisionShape.toAabbs()) {
                        AABB worldBox = localBox.move(pos);
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
            if (!level.noCollision(null, targetBox, true)) {
                continue;
            }
            if (level.noCollision(null, targetBox.move(0, -0.05, 0), true)) {
                continue;
            }

            AABB startBox = new AABB(
                    from.x - halfWidth, from.y, from.z - halfWidth,
                    from.x + halfWidth, from.y + playerHeight, from.z + halfWidth
            );
            boolean reachable;
            if (deltaY > 1e-4) {
                reachable = PathingUtils.isCollisionFreeMove(startBox, new Vec3(0, deltaY, 0))
                        && PathingUtils.isCollisionFreeMove(startBox.move(0, deltaY, 0), new Vec3(to.x - from.x, 0, to.z - from.z));
            } else if (deltaY < -1e-4) {
                reachable = PathingUtils.isCollisionFreeMove(startBox, new Vec3(to.x - from.x, 0, to.z - from.z))
                        && PathingUtils.isCollisionFreeMove(startBox.move(to.x - from.x, 0, to.z - from.z), new Vec3(0, deltaY, 0));
            } else {
                reachable = PathingUtils.isCollisionFreeMove(startBox, new Vec3(to.x - from.x, 0, to.z - from.z));
            }
            if (reachable) {
                return candidateY;
            }
        }

        return Double.NaN;
    }

    private List<Vec3> getAllBlockOffsets(float playerWidth, int offsetX, int offsetZ) {
        return getAllBlockOffsets(playerWidth).stream().map(vec3 -> vec3.add(offsetX, 0, offsetZ)).toList();
    }

    private List<Vec3> getAllBlockOffsets(float playerWidth) {
        playerWidth += 1e-7f;
        List<Vec3> blockOffsets = new ArrayList<>();
        // hugging walls
        blockOffsets.add(new Vec3(playerWidth / 2, 0, 0));
        blockOffsets.add(new Vec3(1 - playerWidth / 2, 0, 0));
        blockOffsets.add(new Vec3(0, 0, playerWidth / 2));
        blockOffsets.add(new Vec3(0, 0, 1 - playerWidth / 2));
        blockOffsets.add(new Vec3(1 - playerWidth / 2, 0, 1 - playerWidth / 2));
        blockOffsets.add(new Vec3(playerWidth / 2, 0, 1 - playerWidth / 2));
        blockOffsets.add(new Vec3(1 - playerWidth / 2, 0, playerWidth / 2));
        blockOffsets.add(new Vec3(playerWidth / 2, 0, playerWidth / 2));
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

        return blockOffsets;
    }
}
