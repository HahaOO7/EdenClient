package at.haha007.edenclient.mods.automine;

import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

class TaskMineBlock implements Task {
	private final MinecraftClient MC = MinecraftClient.getInstance();
	private final BlockPos pos;
	private int miningCooldown = -1;

	TaskMineBlock(BlockPos blockPos) {
		pos = blockPos;
	}

	public boolean tick(ClientPlayerEntity player) {
		double distance = player.getPos().squaredDistanceTo(Vec3d.ofCenter(pos));

		//check if in distance to mine
		if (distance > 25) return true;

		//check if the block is already being mined
		if (miningCooldown == -1) {
			return continueMining(player);
		} else {
			return startMining(player);
		}
	}

	private boolean continueMining(ClientPlayerEntity player) {
		if (--miningCooldown <= 0) {
			MC.interactionManager.breakBlock(pos);
			return false;
		}
		return true;
	}

	private boolean startMining(ClientPlayerEntity player) {
		BlockState blockState = player.getEntityWorld().getBlockState(pos);

		float speed = getBestTool(blockState, player);
		if (speed >= 1) {
			MC.interactionManager.breakBlock(pos);
			return false;
		} else {
			MC.getNetworkHandler().sendPacket(
				new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK,
					pos,
					Direction.getFacing(player.getX(), player.getEyeY(), player.getZ()
					)
				)
			);
			miningCooldown = (int) (1 / speed);
			return true;
		}
	}


	private float getBestTool(BlockState blockState, ClientPlayerEntity player) {
		PlayerInventory inventory = player.inventory;
		int startSlot = inventory.selectedSlot;
		float bestMiningSpeed = blockState.calcBlockBreakingDelta(player, MC.world, pos);
		int bestSlot = inventory.selectedSlot;

		for (int i = 0; i < 9; i++) {
			float miningSpeed = blockState.calcBlockBreakingDelta(player, MC.world, pos);
			if (miningSpeed > bestMiningSpeed) {
				bestMiningSpeed = miningSpeed;
				bestSlot = i;
			}
		}

		if (startSlot != bestSlot) {
			MC.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(bestSlot));
		} else {
			inventory.selectedSlot = startSlot;
		}
		return bestMiningSpeed;
	}

}
