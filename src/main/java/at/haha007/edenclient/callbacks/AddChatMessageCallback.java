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
            listeners -> (event) -> {
                for (AddChatMessageCallback listener : listeners) {
                    ActionResult result = listener.interact(event);

                    if (result != ActionResult.PASS) {
                        return result;
                    }
                }

                return ActionResult.PASS;
            });

    ActionResult interact(ChatAddEvent event);

    public static class ChatAddEvent {
        private final ClientPlayerEntity player;
        private Text chatText;
        private final int chatLineId;
        private final List<ChatHudLine<OrderedText>> chatLines;

        public ChatAddEvent(ClientPlayerEntity player, Text chatText, int chatLineId, List<ChatHudLine<OrderedText>> chatLines) {
            this.player = player;
            this.chatText = chatText;
            this.chatLineId = chatLineId;
            this.chatLines = chatLines;
        }

        public Text getChatText() {
            return chatText;
        }

        public List<ChatHudLine<OrderedText>> getChatLines() {
            return chatLines;
        }

        public int getChatLineId() {
            return chatLineId;
        }

        public ClientPlayerEntity getPlayer() {
            return player;
        }

        public void setChatText(Text chatText) {
            this.chatText = chatText;
        }
    }
}
