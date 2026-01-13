package at.haha007.edenclient.mixin;

import at.haha007.edenclient.EdenClient;
import at.haha007.edenclient.mixinterface.ChatComponentAccessor;
import net.minecraft.client.GuiMessage;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
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

    @Inject(at = @At("HEAD"), method = "addMessage(Lnet/minecraft/network/chat/Component;)V", cancellable = true)
    private void onAddMessage(Component text, CallbackInfo ci) {
        ci.cancel();
        EdenClient.chatMessagesToHandle.add(text);
    }

    @Override
    public List<GuiMessage.Line> edenClient$getTrimmedMessages() {
        return this.trimmedMessages;
    }
}
