package at.haha007.edenclient.mixin;

import at.haha007.edenclient.EdenClient;
import at.haha007.edenclient.callbacks.AddChatMessageCallback;
import at.haha007.edenclient.mixinterface.IVisibleMessageAccessor;
import at.haha007.edenclient.utils.PlayerUtils;
import com.mojang.logging.LogUtils;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.client.gui.hud.MessageIndicator;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.network.message.MessageSignatureData;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;


@Mixin(ChatHud.class)
public abstract class ChatHudMixin extends DrawableHelper implements IVisibleMessageAccessor {

    @Final
    @Shadow
    private List<ChatHudLine.Visible> visibleMessages;

    @Shadow
    public abstract void addMessage(Text message, @Nullable MessageSignatureData signature, @Nullable MessageIndicator indicator);

    @Inject(at = @At("HEAD"),
            method = "addMessage(Lnet/minecraft/text/Text;)V", cancellable = true)
    private void onAddMessage(Text text, CallbackInfo ci) {
        ci.cancel();
        EdenClient.chatThread.submit(() -> {
            Text chatText = text;
            ClientPlayerEntity player = PlayerUtils.getPlayer();
            AddChatMessageCallback.ChatAddEvent event = new AddChatMessageCallback.ChatAddEvent(player, chatText, visibleMessages);
            try {
                AddChatMessageCallback.EVENT.invoker().interact(event);
            } catch (Throwable t) {
                t.printStackTrace();
            }
            chatText = event.getChatText();
            if (chatText != null && !chatText.getString().isBlank()) {
                addMessage(chatText, null, MessageIndicator.system());
                LogUtils.getLogger().info("Chat: " + Text.Serializer.toJson(chatText));
            }
        });
    }
}
