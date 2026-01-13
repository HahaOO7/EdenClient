package at.haha007.edenclient.mixin;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.chat.ChatListener;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.PlayerChatMessage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.time.Instant;

@Mixin(ChatListener.class)
public class ChatListenerMixin {
    @Inject(method = "showMessageToPlayer", at = @At("HEAD"), cancellable = true)
    public void onShowMessageToPlayer(ChatType.Bound bound,
                                      PlayerChatMessage playerChatMessage,
                                      Component component,
                                      GameProfile gameProfile,
                                      boolean bl,
                                      Instant instant,
                                      CallbackInfoReturnable<Boolean> cir) {
        Minecraft.getInstance().gui.getChat().addMessage(component);
        cir.setReturnValue(true);
    }

}
