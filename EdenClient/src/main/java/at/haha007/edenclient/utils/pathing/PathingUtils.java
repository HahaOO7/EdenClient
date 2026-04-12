package at.haha007.edenclient.utils.pathing;

import at.haha007.edenclient.utils.PlayerUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.CubeVoxelShape;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.List;
import java.util.Optional;

public class PathingUtils {

    private PathingUtils() {
    }

    public static boolean canJumpReach(BlockPos from, BlockPos to) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return false;
        LocalPlayer player = PlayerUtils.getPlayer();
        VoxelShape fromShape = level.getBlockState(from).getCollisionShape(level, from, CollisionContext.of(player));
        VoxelShape toShape = level.getBlockState(to).getCollisionShape(level, to, CollisionContext.of(player));
        float jumpPower = getJumpPower();
        // Assume sprint-jump speed for reachability checks without mutating player state.
        player.setSprinting(true);
        double walkingSpeed = PlayerUtils.getWalkingSpeed() * .91;
        EntityDimensions playerDimensions = player.getDimensions(player.getPose());

        if (fromShape.isEmpty() || toShape.isEmpty()) {
            return false;
        }

        double startY = from.getY() + fromShape.max(Direction.Axis.Y);
        double targetY = to.getY() + toShape.max(Direction.Axis.Y);

        AABB fromBox = fromShape.bounds().move(from.getX(), from.getY(), from.getZ());
        AABB toBox = toShape.bounds().move(to.getX(), to.getY(), to.getZ());

        // Horizontal face-to-face block gap.
        double dx = Math.max(0, Math.max(fromBox.minX - toBox.maxX, toBox.minX - fromBox.maxX) - playerDimensions.width() * 2);
        double dz = Math.max(0, Math.max(fromBox.minZ - toBox.maxZ, toBox.minZ - fromBox.maxZ) - playerDimensions.width() * 2);
        double horizontalGap = Math.sqrt(dx * dx + dz * dz);
        // The player can take off/land near edges, reducing required center movement by roughly body width.
        double horizontalDistance = Math.max(0, horizontalGap);
        //find the amount of ticks the player needs to walk the distance
        int ticks = (int) Math.ceil(horizontalDistance / walkingSpeed);

        // simulate the jump to see if the player falls below the target block before the ticks are reached
        double speedY = jumpPower;
        for (int i = 0; i < ticks; i++) {
            startY += speedY;
            speedY -= 0.08; // gravity
            speedY *= 0.98; // drag
            if (startY < targetY && speedY < 0) {
                return false;
            }
        }

        return true;
    }

    public static boolean isCollisionFreeMove(AABB shape, Vec3 movement) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return false;

        double length = movement.length();

        // Shape is already at the starting position.
        if (!level.noCollision(shape)) return false;
        if (length < 1e-6) return true;

        Vec3 direction = movement.normalize();
        double stepSize = 0.1;
        int steps = (int) Math.ceil(length / stepSize);

        for (int i = 1; i <= steps; i++) {
            double t = Math.min(i * stepSize, length);
            AABB swept = shape.move(direction.scale(t));
            if (!level.noCollision(swept)) return false;
        }

        return true;
    }

    public static float getJumpPower() {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return 0;
        LocalPlayer player = PlayerUtils.getPlayer();
        float f = level.getBlockState(player.blockPosition()).getBlock().getJumpFactor();
        float g = level.getBlockState(player.getBlockPosBelowThatAffectsMyMovement()).getBlock().getJumpFactor();
        float blockJumpFactor = f == 1.0 ? g : f;
        return (float) player.getAttributeValue(Attributes.JUMP_STRENGTH) * blockJumpFactor + player.getJumpBoostPower();
    }

    public static Optional<Double> getWalkableHeight(BlockPos pos) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return Optional.empty();

        BlockState blockState = level.getBlockState(pos);
        VoxelShape collisionShape = blockState.getCollisionShape(level, pos);
        List<AABB> aabbs = collisionShape.toAabbs();
        //check if floor has no collisions
        if (aabbs.isEmpty()) {
            return Optional.empty();
        }

        LocalPlayer player = PlayerUtils.getPlayer();
        EntityDimensions dimensions = player.getDimensions(player.getPose());
        double minSize = 1 - dimensions.width() / 3;

        //check if floor is big enough
        AABB bounds = collisionShape.bounds();
        if (bounds.getXsize() < minSize || bounds.getZsize() < minSize) {
            return Optional.empty();
        }
        //check if there is enough headroom
        double playerHeight = dimensions.height();
        double floorHeight = bounds.maxY;
        Vec3 floorPos = new Vec3(pos.getX() + .5, floorHeight + pos.getY(), pos.getZ() + .5);
        float halfWidth = dimensions.width() / 2;
        AABB standingAABB = new AABB(floorPos.add(-halfWidth, 0, -halfWidth), floorPos.add(halfWidth, playerHeight, halfWidth));
        boolean noCollision = level.noCollision(null, standingAABB, true);
        return noCollision ? Optional.of(floorPos.y) : Optional.empty();
    }
}
