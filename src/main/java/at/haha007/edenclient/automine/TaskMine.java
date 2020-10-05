package at.haha007.edenclient.automine;

import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.LinkedList;
import java.util.Queue;

public class TaskMine implements Task {
	private MinecraftClient mc = MinecraftClient.getInstance();
	private BlockPos targetPos;
	private Vec3d targetCenter;
	private Queue<BlockPos> path = new LinkedList<>();
	private BlockBox box;
	private ClientPlayerEntity player;
	private int miningCooldown;

	public void setBox(BlockBox box) {
		this.box = box;
	}

	public boolean tick(ClientPlayerEntity player) {
		this.player = player;
		if ((targetPos == null || player.world.getBlockState(targetPos).isAir()) && !nextTarget()) return false;

		walkTowardsTarget(player.getMovementSpeed() * 0.1f);
		mineTarget();
		return true;
	}

	private void mineTarget() {
		if (player.getPos().distanceTo(targetCenter) > 5) return;
		if (miningCooldown > 0) {
			//continueMining
			if (--miningCooldown <= 0) {
				mc.interactionManager.breakBlock(targetPos);
			}
		} else {
			//StartMining
			BlockState blockState = player.getEntityWorld().getBlockState(targetPos);

			float speed = getBestTool(blockState);
			if (speed >= 1) {
				mc.interactionManager.breakBlock(targetPos);
			} else {
				mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, targetPos, Direction.getFacing(player.getX(), player.getEyeY(), player.getZ())));
				miningCooldown = (int) (1 / speed);
			}
		}
	}

	private void walkTowardsTarget(float speed) {
		if (path.isEmpty()) {
			if (targetPos != null) path.add(targetPos);
			else return;
		}
		Vec3d next = Vec3d.ofBottomCenter(path.poll());
		next = next.subtract(player.getPos());
		if (next.length() > speed) {

		} else {
			player.setPos(next.x, next.y, next.z);
			walkTowardsTarget((float) (speed - next.length()));
		}
	}

	private float getBestTool(BlockState blockState) {
		PlayerInventory inventory = player.inventory;
		int startSlot = inventory.selectedSlot;
		float bestMiningSpeed = blockState.calcBlockBreakingDelta(player, mc.world, targetPos);
		int bestSlot = inventory.selectedSlot;

		for (int i = 0; i < 9; i++) {
			float miningSpeed = blockState.calcBlockBreakingDelta(player, mc.world, targetPos);
			if (miningSpeed > bestMiningSpeed) {
				bestMiningSpeed = miningSpeed;
				bestSlot = i;
			}
		}

		if (startSlot != bestSlot) {
			mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(bestSlot));
		} else {
			inventory.selectedSlot = startSlot;
		}
		return bestMiningSpeed;
	}


	public void setTargetPos(BlockPos targetPos) {
		this.targetPos = targetPos;
		targetCenter = Vec3d.ofBottomCenter(targetPos);
	}

	private boolean nextTarget() {
		//return false when there is no target


		return false;
	}


}
