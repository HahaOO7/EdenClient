package at.haha007.edenclient.mixin;

import at.haha007.edenclient.EdenClient;
import at.haha007.edenclient.callbacks.JoinWorldCallback;
import at.haha007.edenclient.callbacks.LeaveWorldCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ReceivingLevelScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftClientMixin {

    @Shadow @Nullable public ClientLevel level;
    @Unique
    boolean connected;

    @Inject(at = @At("TAIL"), method = "setLevel")
    void onDisconnect(ClientLevel clientLevel, ReceivingLevelScreen.Reason reason, CallbackInfo ci) {
        boolean connect = clientLevel != null;
        if (connect == this.connected) return;
        if (connect) {
            EdenClient.onJoin();
            JoinWorldCallback.EVENT.invoker().join();
        } else {
            LeaveWorldCallback.EVENT.invoker().leave();
            EdenClient.onQuit();
        }
        this.connected = connect;
    }

    @Inject(at = @At("TAIL"), method = "disconnect(Lnet/minecraft/client/gui/screens/Screen;)V")
    void onDisconnect(Screen screen, CallbackInfo ci) {
        if (!this.connected) return;
        connected = false;
        LeaveWorldCallback.EVENT.invoker().leave();
        EdenClient.onQuit();
    }
}
