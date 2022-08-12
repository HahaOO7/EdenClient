package at.haha007.edenclient.callbacks;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;

import java.util.List;

public interface AddChatMessageCallback {
    Event<AddChatMessageCallback> EVENT = EventFactory.createArrayBacked(AddChatMessageCallback.class,
            listeners -> event -> {
                for (AddChatMessageCallback listener : listeners) {
                    listener.interact(event);
                }
            });

    void interact(ChatAddEvent event);

    class ChatAddEvent {
        private final ClientPlayerEntity player;
        private Text chatText;
        private final List<ChatHudLine.Visible> chatLines;

        public ChatAddEvent(ClientPlayerEntity player, Text chatText, List<ChatHudLine.Visible> chatLines) {
            this.player = player;
            this.chatText = chatText;
            this.chatLines = chatLines;
        }

        public Text getChatText() {
            return chatText;
        }

        public List<ChatHudLine.Visible> getChatLines() {
            return chatLines;
        }

        public ClientPlayerEntity getPlayer() {
            return player;
        }

        public void setChatText(Text chatText) {
            this.chatText = chatText;
        }
    }
}
