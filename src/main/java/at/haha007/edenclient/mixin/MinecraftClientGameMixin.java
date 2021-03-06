package at.haha007.edenclient.mixin;

import at.haha007.edenclient.callbacks.LeaveGameSessionCallback;
import at.haha007.edenclient.callbacks.StartGameSessionCallback;
import com.mojang.bridge.launcher.SessionEventListener;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.MinecraftClientGame;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClientGame.class)
public class MinecraftClientGameMixin {

    @Inject(method = "onStartGameSession", at = @At("TAIL"))
    void onStartGameSession(CallbackInfo ci) {
        StartGameSessionCallback.EVENT.invoker().join(MinecraftClient.getInstance().player);
    }

    @Inject(method = "onLeaveGameSession", at = @At("TAIL"))
    void onLeaveGameSession(CallbackInfo ci) {
        LeaveGameSessionCallback.EVENT.invoker().leave(MinecraftClient.getInstance().player);
    }
}
