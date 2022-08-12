package at.haha007.edenclient.mixin;

import at.haha007.edenclient.callbacks.AddChatMessageCallback;
import at.haha007.edenclient.utils.PlayerUtils;
import com.google.gson.annotations.JsonAdapter;
import com.mojang.logging.LogUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.client.gui.hud.MessageIndicator;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.network.message.MessageSignatureData;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.JsonSerializing;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.FabricUtil;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.security.Signature;
import java.util.List;
import java.util.concurrent.*;


@Mixin(ChatHud.class)
public abstract class ChatHudMixin extends DrawableHelper {

    @Final
    @Shadow
    private List<ChatHudLine.Visible> visibleMessages;

    private final ExecutorService thread = Executors.newSingleThreadExecutor();

    @Shadow
    public abstract void addMessage(Text message, @Nullable MessageSignatureData signature, @Nullable MessageIndicator indicator) ;

    @Inject(at = @At("HEAD"),
            method = "addMessage(Lnet/minecraft/text/Text;)V", cancellable = true)
    private void onAddMessage(Text text, CallbackInfo ci) {
        ci.cancel();
        thread.submit(() -> {
            Text chatText = text;
            ClientPlayerEntity player = PlayerUtils.getPlayer();
            AddChatMessageCallback.ChatAddEvent event = new AddChatMessageCallback.ChatAddEvent(player, chatText,  visibleMessages);
            try {
                AddChatMessageCallback.EVENT.invoker().interact(event);
            }catch (Throwable t){
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
