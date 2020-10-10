package at.haha007.edenclient.mods.automine;

import at.haha007.edenclient.callbacks.PlayerTickCallback;
import at.haha007.edenclient.command.Command;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.LinkedList;
import java.util.Queue;

public class AutoMiner {

	private final MinecraftClient client = MinecraftClient.getInstance();
	private boolean enabled = false;
	private boolean toolEnabled = false;
	private static final Item tool = Items.STICK;
	private BlockBox selection;
	private final Queue<Task> tasks = new LinkedList<>();
	private int task = 0;

	public AutoMiner() {
		PlayerTickCallback.EVENT.register(this::tick);
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public void setBox(BlockBox selection) {
		this.selection = selection;
	}

	public ActionResult tick(ClientPlayerEntity entity) {
		return ActionResult.PASS;
	}

	public void onCommand(Command command, String s, String[] strings) {
		toolEnabled = !toolEnabled;
	}

	public boolean attackBlock(BlockPos pos, Direction direction) {
		if (!toolEnabled) return false;
		if (getPlayer().inventory.getMainHandStack().getItem() != tool) return false;

		setBox(new BlockBox(pos, pos));
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

		setBox(selection);
		return true;
	}

	private ClientPlayerEntity getPlayer() {
		return client.player;
	}
}
