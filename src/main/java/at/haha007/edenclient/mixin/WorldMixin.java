package at.haha007.edenclient.mixin;

import at.haha007.edenclient.callbacks.LeaveGameSessionCallback;
import at.haha007.edenclient.callbacks.StartGameSessionCallback;
import net.minecraft.client.MinecraftClient;
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
        for (int i = 0; i < 10; i++) {
            System.out.println("LOAD");
        }
        StartGameSessionCallback.EVENT.invoker().join(MinecraftClient.getInstance().player);
        connected = true;
    }

    @Inject(at = @At("HEAD"), method = "disconnect")
    private void onDisconnect(CallbackInfo ci) {
        for (int i = 0; i < 10; i++) {
            System.out.println("SAVE");
        }
        LeaveGameSessionCallback.EVENT.invoker().leave(MinecraftClient.getInstance().player);
        connected = false;
    }
}
