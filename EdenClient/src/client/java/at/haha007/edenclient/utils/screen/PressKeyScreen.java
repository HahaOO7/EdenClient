package at.haha007.edenclient.utils.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;

import java.util.function.IntConsumer;

public class PressKeyScreen extends ShowTextScreen{
    private final IntConsumer action;

    public PressKeyScreen(IntConsumer action) {
        super(Component.literal("Press a key"));
        this.action = action;
    }

    @Override
    public boolean keyPressed(KeyEvent keyEvent) {
        action.accept(keyEvent.key());
        Minecraft.getInstance().setScreen(null);
        return true;
    }
}
