package at.haha007.edenclient.mixin;

import at.haha007.edenclient.callbacks.AddChatMessageCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;


@Mixin(ChatHud.class)
public abstract class ChatHudMixin extends DrawableHelper {

    @Final
    @Shadow
    private List<ChatHudLine<OrderedText>> visibleMessages;

    @Shadow
    protected abstract void addMessage(Text stringRenderable, int messageId, int timestamp, boolean bl);

    @Inject(at = @At("HEAD"),
            method = "addMessage(Lnet/minecraft/text/Text;I)V", cancellable = true)
    private void onAddMessage(Text chatText, int chatLineId, CallbackInfo ci) {
        AddChatMessageCallback.ChatAddEvent event = new AddChatMessageCallback.ChatAddEvent(MinecraftClient.getInstance().player, chatText, chatLineId, visibleMessages);
        AddChatMessageCallback.EVENT.invoker().interact(event);
        chatText = event.getChatText();
        ci.cancel();
        if (chatText != null) {
            addMessage(chatText, chatLineId, MinecraftClient.getInstance().inGameHud.getTicks(), false);
            System.out.println("Chat: " + chatText.getString());
        }
    }
}
