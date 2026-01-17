package at.haha007.edenclient.mixin;

import at.haha007.edenclient.callbacks.ChatKeyCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.util.ArrayListDeque;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChatScreen.class)
public abstract class ChatScreenMixin {
    @Shadow
    public abstract void moveInHistory(int i);

    @Shadow
    protected EditBox input;

    @Shadow
    private int historyPos;

    @Inject(at = @At("HEAD"), method = "keyPressed", cancellable = true)
    private void onKeyPressed(KeyEvent keyEvent, CallbackInfoReturnable<Boolean> cir) {
        ArrayListDeque<String> recentChat = Minecraft.getInstance().gui.getChat().getRecentChat();
        String prefix = input.getValue().substring(0, input.getCursorPosition());
        int newPos = ChatKeyCallback.EVENT.invoker().getNewPosInHistory(keyEvent.key(), recentChat, prefix, historyPos);
        if (newPos < 0 || newPos > recentChat.size() - 1 || historyPos == newPos) return;
        moveInHistory(newPos - historyPos);
        input.setCursorPosition(prefix.length());
        cir.setReturnValue(true);
    }
}
