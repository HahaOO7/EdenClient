package at.haha007.edenclient.mixin;

import at.haha007.edenclient.callbacks.LeaveWorldCallback;
import at.haha007.edenclient.callbacks.JoinWorldCallback;
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
    ClientWorld connectedWorld;

    @Inject(at = @At("TAIL"), method = "setWorld")
    void onDisconnect(ClientWorld world, CallbackInfo ci) {
        if (world == connectedWorld) return;
        if (world != null) {
            JoinWorldCallback.EVENT.invoker().join(world);
        } else {
            LeaveWorldCallback.EVENT.invoker().leave(connectedWorld);
        }
        connectedWorld = world;
    }
}
