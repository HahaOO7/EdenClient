package at.haha007.edenclient.utils;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

public class PlayerUtils {

	public static void sendMessage(Text text) {
		MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(text);
		System.out.println(text.getString());

	}
}
