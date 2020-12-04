package at.haha007.edenclient.mods;

import at.haha007.edenclient.command.Command;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.text.LiteralText;
import net.minecraft.world.chunk.WorldChunk;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class AutoSell {
	private final Set<Item> autoSellItems = new HashSet<>();
	private long lastSell = 0;

	public void onCommand(Command command, String label, String[] args) {

		if (args.length != 1) {
			sendChatMessage("/autosell add");
			sendChatMessage("/autosell remove");
			sendChatMessage("/autosell reset");
			return;
		}
		ClientPlayerEntity player = MinecraftClient.getInstance().player;
		if (player == null) return;
		PlayerInventory inventory = player.inventory;

		switch (args[0]) {
			case "add":
				autoSellItems.add(inventory.getMainHandStack().getItem());
				sendChatMessage("added /sell " + inventory.getMainHandStack().getItem().getName().getString());
				break;
			case "remove":
				autoSellItems.remove(inventory.getMainHandStack().getItem());
				sendChatMessage("removed /sell " + inventory.getMainHandStack().getItem().getName().getString());
				break;
			case "list":
				sendChatMessage(autoSellItems.toString());
				break;
			case "reset":
				autoSellItems.clear();
				sendChatMessage("Removed all entries");
				break;
			default:
				sendChatMessage("/autosell add");
				sendChatMessage("/autosell remove");
				sendChatMessage("/autosell reset");
				sendChatMessage("/autosell list");
		}
	}


	public void onInventoryChange() {
		ClientPlayerEntity player = MinecraftClient.getInstance().player;
		if (player == null) return;
		PlayerInventory inventory = player.inventory;
		if (!isFullInventory(inventory)) return;
		executeAutoSell(player);
	}

	private void executeAutoSell(ClientPlayerEntity player) {
		long time = System.currentTimeMillis();
		if (time - 200 < lastSell) return;
		autoSellItems
			.stream()
			.filter(item -> player.inventory.containsAny(Collections.singleton(item)))
			.forEach(item -> player.sendChatMessage("/sell " + item.getName().getString()));
		lastSell = time;
	}

	private boolean isFullInventory(PlayerInventory inventory) {
		return inventory.getEmptySlot() == -1;
	}


	private void sendChatMessage(String text) {
		MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(new LiteralText(text));
	}
}
