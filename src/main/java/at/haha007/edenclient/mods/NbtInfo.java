package at.haha007.edenclient.mods;

import at.haha007.edenclient.command.Command;
import at.haha007.edenclient.command.CommandManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.visitor.NbtTextFormatter;
import net.minecraft.text.LiteralText;
import net.minecraft.text.NbtText;

import static at.haha007.edenclient.utils.PlayerUtils.sendMessage;

public class NbtInfo {

	public NbtInfo() {
		CommandManager.registerCommand(new Command(this::onCommand), "nbtinfo", "nbt");
	}

	public void onCommand(Command command, String label, String[] args) {
		ClientPlayerEntity player = MinecraftClient.getInstance().player;
		assert player != null;
		PlayerInventory inv = player.getInventory();
		ItemStack stack = inv.getMainHandStack();
		if (stack.isEmpty()) {
			sendMessage(new LiteralText("Item in die Hand!"));
		} else {
			sendMessage(new NbtTextFormatter("", 1).apply(stack.getOrCreateTag()));
		}
	}
}
