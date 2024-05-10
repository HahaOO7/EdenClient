package at.haha007.edenclient.callbacks;

import net.minecraft.client.GuiMessage;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

import java.util.List;

public interface AddChatMessageCallback {
    Event<AddChatMessageCallback> EVENT = new Event<>(
            listeners -> event -> {
                for (AddChatMessageCallback listener : listeners) {
                    listener.interact(event);
                }
            });

    void interact(ChatAddEvent event);

    class ChatAddEvent {
        private final LocalPlayer player;
        private Component chatText;
        private final List<GuiMessage.Line> chatLines;

        public ChatAddEvent(LocalPlayer player, Component chatText, List<GuiMessage.Line> chatLines) {
            this.player = player;
            this.chatText = chatText;
            this.chatLines = chatLines;
        }

        public Component getChatText() {
            return chatText;
        }

        public List<GuiMessage.Line> getChatLines() {
            return chatLines;
        }

        public LocalPlayer getPlayer() {
            return player;
        }

        public void setChatText(Component chatText) {
            this.chatText = chatText;
        }
    }
}
