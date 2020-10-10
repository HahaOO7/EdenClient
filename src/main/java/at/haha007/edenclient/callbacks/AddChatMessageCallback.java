package at.haha007.edenclient.callbacks;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;

import java.util.List;

public interface AddChatMessageCallback {
	Event<AddChatMessageCallback> EVENT = EventFactory.createArrayBacked(AddChatMessageCallback.class,
		listeners -> (player, text, lineId, chatLines) -> {
			for (AddChatMessageCallback listener : listeners) {
				ActionResult result = listener.interact(player, text, lineId, chatLines);

				if (result != ActionResult.PASS) {
					return result;
				}
			}

			return ActionResult.PASS;
		});

	ActionResult interact(ClientPlayerEntity player, Text chatText, int chatLineId, List<ChatHudLine<OrderedText>> chatLines);
}
