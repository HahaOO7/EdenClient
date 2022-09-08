package at.haha007.edenclient.mixinterface;

import net.minecraft.client.gui.hud.ChatHudLine;

import java.util.List;

public interface IVisibleMessageAccessor {
    List<ChatHudLine.Visible> getVisibleMessages();
}
