package at.haha007.edenclient.mixinterface;

import net.minecraft.client.GuiMessage;

import java.util.List;

public interface ChatComponentAccessor {
    List<GuiMessage.Line> edenClient$getTrimmedMessages();
}
