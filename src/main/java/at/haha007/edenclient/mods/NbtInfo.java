package at.haha007.edenclient.mods;

import at.haha007.edenclient.command.Command;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;

public class NbtInfo {

	public void onCommand(Command command, String label, String[] args) {
		ClientPlayerEntity player = MinecraftClient.getInstance().player;
		assert player != null;
		PlayerInventory inv = player.inventory;
		ItemStack stack = inv.getMainHandStack();
		if(stack.isEmpty()){
			sendMessage(new LiteralText("Item in die Hand!"));
		}else{
			sendMessage(stack.getOrCreateTag().toText());
		}
	}

	private void sendMessage(Text text) {
		MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(text);
		System.out.println(text.getString());
	}
}
