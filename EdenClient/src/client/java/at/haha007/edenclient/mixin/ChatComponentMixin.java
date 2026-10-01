package at.haha007.edenclient.mixin;

import at.haha007.edenclient.EdenClient;
import at.haha007.edenclient.mixinterface.ChatComponentAccessor;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.client.multiplayer.chat.GuiMessageSource;
import net.minecraft.client.multiplayer.chat.GuiMessageTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;


@Mixin(ChatComponent.class)
public abstract class ChatComponentMixin implements ChatComponentAccessor {

    @Final
    @Shadow
    private List<GuiMessage.Line> trimmedMessages;

    @Inject(at = @At("HEAD"), method = "addPlayerMessage", cancellable = true)
    private void onAddMessage(Component message, MessageSignature signature, GuiMessageTag tag, CallbackInfo ci) {
        ci.cancel();
        EdenClient.chatMessagesToHandle.add(message);
    }
    @Inject(at = @At("HEAD"), method = "addServerSystemMessage", cancellable = true)
    private void onAddMessage(Component message, CallbackInfo ci) {
        ci.cancel();
        EdenClient.chatMessagesToHandle.add(message);
    }

    @Override
    public List<GuiMessage.Line> edenClient$getTrimmedMessages() {
        return this.trimmedMessages;
    }
}
