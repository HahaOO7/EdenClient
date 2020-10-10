package at.haha007.edenclient.mods.automine;

import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class TaskPlaceBlock implements Task {
	private final BlockPos pos;
	private final MinecraftClient MC = MinecraftClient.getInstance();

	TaskPlaceBlock(BlockPos targetPos) {
		pos = targetPos;
	}

	public boolean tick(ClientPlayerEntity player) {

		double distance = player.getPos().squaredDistanceTo(Vec3d.ofCenter(pos));

		if (distance > 25) return true;
		World world = player.clientWorld;
		BlockState blockState = world.getBlockState(pos);
		player.clientWorld.raycastBlock(player.getCameraPosVec(0f), Vec3d.ofCenter(pos), pos, blockState.getCollisionShape(world, pos), blockState);

		MC.getNetworkHandler().sendPacket(
			new PlayerInteractBlockC2SPacket(
				Hand.MAIN_HAND,
				new BlockHitResult(player.getPos(), Direction.UP, pos, false)
			)
		);

		MC.getNetworkHandler().sendPacket(
			new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK,
				pos,
				Direction.getFacing(player.getX(), player.getEyeY(), player.getZ()
				)
			)
		);

		return false;
	}
}
