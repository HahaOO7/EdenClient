package at.haha007.edenclient.automine;

import at.haha007.edenclient.command.Command;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.ArrayList;
import java.util.List;

public class AutoMiner {

	private final MinecraftClient client = MinecraftClient.getInstance();
	private boolean toolEnabled = false;
	private static final Item tool = Items.STICK;
	private BlockBox selection;
	private final List<Task> tasks = new ArrayList<>();
	private int task = 0;

	public AutoMiner() {
		tasks.add(new TaskMine());
		tasks.add(new TaskCooldown(10));
		tasks.add(new TaskLiquids());
		tasks.add(new TaskCooldown(50));
		tasks.add(new TaskDrop());
		tasks.add(new TaskCooldown(50));
	}

	public void tick() {
		if (!toolEnabled) return;
		selection = BlockBox.infinite();
		if (!tasks.get(task % tasks.size()).tick(getPlayer())) task++;
	}

	public void onCommand(Command command, String s, String[] strings) {
		toolEnabled = !toolEnabled;
	}

	public boolean attackBlock(BlockPos pos, Direction direction) {
		if (!toolEnabled) return false;
		if (getPlayer().inventory.getMainHandStack().getItem() != tool) return false;

		selection = new BlockBox(pos, pos);
		return true;
	}

	public boolean interactBlock(ClientWorld world, BlockHitResult hitResult) {
		if (!toolEnabled) return false;
		if (getPlayer().inventory.getMainHandStack().getItem() != tool) return false;

		BlockPos blockPos = hitResult.getBlockPos();

		selection.minX = Math.min(selection.minX, blockPos.getX());
		selection.minY = Math.min(selection.minY, blockPos.getY());
		selection.minZ = Math.min(selection.minZ, blockPos.getZ());

		selection.maxX = Math.max(selection.maxX, blockPos.getX());
		selection.maxY = Math.max(selection.maxY, blockPos.getY());
		selection.maxZ = Math.max(selection.maxZ, blockPos.getZ());

		return true;
	}

	private ClientPlayerEntity getPlayer() {
		return client.player;
	}
}
