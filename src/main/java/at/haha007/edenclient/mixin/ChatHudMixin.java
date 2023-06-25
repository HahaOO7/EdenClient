package at.haha007.edenclient.mixin;

import at.haha007.edenclient.EdenClient;
import at.haha007.edenclient.callbacks.AddChatMessageCallback;
import at.haha007.edenclient.mixinterface.IVisibleMessageAccessor;
import at.haha007.edenclient.utils.PlayerUtils;
import com.mojang.logging.LogUtils;
import net.minecraft.client.GuiMessage;
import net.minecraft.client.GuiMessageTag;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;


@Mixin(ChatComponent.class)
public abstract class ChatHudMixin implements IVisibleMessageAccessor {

    @Final
    @Shadow
    private List<GuiMessage.Line> trimmedMessages;

    @Shadow
    public abstract void addMessage(Component message, @Nullable MessageSignature signature, @Nullable GuiMessageTag indicator);

    @Inject(at = @At("HEAD"), method = "addMessage(Lnet/minecraft/network/chat/Component;)V", cancellable = true)
    private void onAddMessage(Component text, CallbackInfo ci) {
        ci.cancel();
        EdenClient.chatThread.submit(() -> {
            Component chatText = text;
            LocalPlayer player = PlayerUtils.getPlayer();
            AddChatMessageCallback.ChatAddEvent event = new AddChatMessageCallback.ChatAddEvent(player, chatText, trimmedMessages);
            try {
                AddChatMessageCallback.EVENT.invoker().interact(event);
            } catch (Throwable t) {
                t.printStackTrace();
            }
            chatText = event.getChatText();
            if (chatText != null && !chatText.getString().isBlank()) {
                addMessage(chatText, null, GuiMessageTag.system());
                LogUtils.getLogger().info("Chat: " + Component.Serializer.toJson(chatText));
            }
        });
    }
}
