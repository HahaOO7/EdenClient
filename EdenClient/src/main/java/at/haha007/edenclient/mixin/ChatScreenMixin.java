package at.haha007.edenclient.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.util.ArrayListDeque;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
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
    private void onKeyPressed(int i, int j, int k, CallbackInfoReturnable<Boolean> cir) {
        if (266 == i) {
            scrollBack();
            cir.setReturnValue(true);
        }
        if (267 == i) {
            scrollForward();
            cir.setReturnValue(true);
        }
    }

    @Unique
    private void scrollBack() {
        ArrayListDeque<String> recentChat = Minecraft.getInstance().gui.getChat().getRecentChat();
        String prefix = input.getValue().substring(0, input.getCursorPosition());
        int posInHistory = historyPos;
        if (posInHistory == 0) return;
        for (int i = posInHistory - 1; i >= 0; i--) {
            if (recentChat.get(i).startsWith(prefix)) {
                moveInHistory(i - posInHistory);
                input.setCursorPosition(prefix.length());
                return;
            }
        }
    }

    @Unique
    private void scrollForward() {
        ArrayListDeque<String> recentChat = Minecraft.getInstance().gui.getChat().getRecentChat();
        String prefix = input.getValue().substring(0, input.getCursorPosition());
        int posInHistory = historyPos;
        if (posInHistory > recentChat.size() - 2) return;
        for (int i = posInHistory + 1; i < recentChat.size(); i++) {
            if (recentChat.get(i).startsWith(prefix)) {
                moveInHistory(i - posInHistory);
                input.setCursorPosition(prefix.length());
                return;
            }
        }
    }
}
