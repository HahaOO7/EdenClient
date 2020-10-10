package at.haha007.edenclient.mods.automine;

import at.haha007.edenclient.callbacks.PlayerAttackBlockCallback;
import at.haha007.edenclient.callbacks.PlayerInteractBlockEvent;
import at.haha007.edenclient.callbacks.PlayerTickCallback;
import at.haha007.edenclient.command.Command;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.text.LiteralText;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.LinkedList;
import java.util.Queue;

public class AutoMiner {

	private final MinecraftClient MC = MinecraftClient.getInstance();
	private boolean enabled = false;
	private boolean toolEnabled = false;
	private static final Item tool = Items.STICK;
	private BlockBox selection;
	private final Queue<Task> tasks = new LinkedList<>();
	private final Queue<Vec3d> path = new LinkedList<>();

	public AutoMiner() {
		PlayerTickCallback.EVENT.register(this::tick);
		PlayerAttackBlockCallback.EVENT.register(this::attackBlock);
		PlayerInteractBlockEvent.EVENT.register(this::interactBlock);
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public void setBox(BlockBox selection) {
		this.selection = selection;
	}

	public ActionResult tick(ClientPlayerEntity player) {
		if (tasks.isEmpty()) nextTask();
		if (tasks.isEmpty()) {
			enabled = false;
			sendMessage("autominer disabled");
			return ActionResult.PASS;
		}
		if (!tasks.peek().tick(player)) tasks.remove();
		if (!path.isEmpty()) tickMovement(player);
		return ActionResult.PASS;
	}

	private void tickMovement(ClientPlayerEntity player) {
		//move the player along the path
	}

	private void nextTask() {
		//find the next target(s)
		//find path towards target
	}


	//Command stuff

	public void onCommand(Command command, String label, String[] args) {
		if (args.length == 0) {
			sendCommandHelp();
			return;
		}
		switch (args[0].toLowerCase()) {
			case "help":
			case "h":
				sendCommandHelp();
				break;
			case "tool":
				toolEnabled = !toolEnabled;
				sendMessage(toolEnabled ? "tool enabled" : "tool disabled");
				break;
			case "start":
				enabled = true;
				tasks.clear();
				sendMessage("autominer enabled");
				break;
			case "stop":
				enabled = false;
				sendMessage("autominer disabled");
				break;
			case "toggle":
				enabled = !enabled;
				sendMessage(enabled ? "autominer enabled" : "autominer disabled");
				if (enabled) tasks.clear();
				break;
		}
	}

	private void sendCommandHelp() {
		sendMessage("/autominer");
	}


	//selection stuff

	public ActionResult attackBlock(ClientPlayerEntity player, BlockPos pos, Direction direction) {
		if (!toolEnabled || enabled) return ActionResult.PASS;
		if (player.inventory.getMainHandStack().getItem() != tool) return ActionResult.PASS;

		setBox(new BlockBox(pos, pos));
		return ActionResult.FAIL;
	}

	public ActionResult interactBlock(ClientPlayerEntity player, ClientWorld world, Hand hand, BlockHitResult hitResult) {
		if (!toolEnabled || enabled) return ActionResult.PASS;
		if (player.inventory.getMainHandStack().getItem() != tool) return ActionResult.PASS;

		BlockPos blockPos = hitResult.getBlockPos();

		selection.minX = Math.min(selection.minX, blockPos.getX());
		selection.minY = Math.min(selection.minY, blockPos.getY());
		selection.minZ = Math.min(selection.minZ, blockPos.getZ());

		selection.maxX = Math.max(selection.maxX, blockPos.getX());
		selection.maxY = Math.max(selection.maxY, blockPos.getY());
		selection.maxZ = Math.max(selection.maxZ, blockPos.getZ());

		setBox(selection);
		return ActionResult.FAIL;
	}

	//utility functions
	private void sendMessage(String message) {
		MC.inGameHud.getChatHud().addMessage(new LiteralText(message));
	}
}
