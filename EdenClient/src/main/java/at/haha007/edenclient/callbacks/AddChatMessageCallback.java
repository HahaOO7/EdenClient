package at.haha007.edenclient.callbacks;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.GuiMessage;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

import java.util.List;

public interface AddChatMessageCallback {
    Event<AddChatMessageCallback> EVENT = new Event<>(
            listeners -> event -> {
                for (AddChatMessageCallback listener : listeners) {
                    listener.onChatAdd(event);
                }
            });

    void onChatAdd(ChatAddEvent event);

    @Getter
    class ChatAddEvent {
        private final LocalPlayer player;
        @Setter
        private Component chatText;
        private final Component unmodifiedChatText;
        private final List<GuiMessage.Line> chatLines;
        @Setter
        private boolean canceled = false;

        public ChatAddEvent(LocalPlayer player, Component chatText, List<GuiMessage.Line> chatLines) {
            this.player = player;
            this.chatText = chatText;
            this.chatLines = chatLines;
            unmodifiedChatText = chatText;
        }

    }
}
