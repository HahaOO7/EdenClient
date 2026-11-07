package at.haha007.edenclient.mixinterface;

import net.minecraft.client.multiplayer.chat.GuiMessage;

import java.util.List;

public interface IChatComponent {
    List<GuiMessage.Line> edenClient$getTrimmedMessages();
}
