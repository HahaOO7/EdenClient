package at.haha007.edenclient.mixin;

import at.haha007.edenclient.callbacks.JoinWorldCallback;
import at.haha007.edenclient.callbacks.LeaveWorldCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {

    @Unique
    boolean connected;

    @Inject(at = @At("TAIL"), method = "setWorld")
    void onDisconnect(ClientWorld world, CallbackInfo ci) {
        boolean connected = world != null;
        if (connected == this.connected) return;
        if (connected) {
            JoinWorldCallback.EVENT.invoker().join();
        } else {
            LeaveWorldCallback.EVENT.invoker().leave();
        }
        this.connected = connected;
    }
}
