package at.haha007.edenclient.mixin;

import at.haha007.edenclient.callbacks.LeaveGameSessionCallback;
import at.haha007.edenclient.callbacks.StartGameSessionCallback;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientWorld.class)
public class WorldMixin {

    @Unique
    private static boolean connected = false;

    @Inject(at = @At("RETURN"), method = "<init>")
    private void onConnect(CallbackInfo ci) {
        if (connected) return;
        StartGameSessionCallback.EVENT.invoker().join();
        connected = true;
    }

    //TODO: doesn't get triggered when getting kicked!
    @Inject(at = @At("HEAD"), method = "disconnect")
    private void onDisconnect(CallbackInfo ci) {
        LeaveGameSessionCallback.EVENT.invoker().leave();
        connected = false;
    }
}
