package at.haha007.edenclient.mixin;

import at.haha007.edenclient.EdenClient;
import at.haha007.edenclient.callbacks.LeaveWorldCallback;
import at.haha007.edenclient.command.CommandManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftClientMixin {

    @Shadow
    @Nullable
    public ClientLevel level;


    @Inject(at = @At("RETURN"), method = "disconnect(Lnet/minecraft/client/gui/screens/Screen;Z)V")
    void onDisconnect(Screen screen, boolean bl, CallbackInfo ci) {
        LeaveWorldCallback.EVENT.invoker().leave();
        EdenClient.onQuit();
        CommandManager.reset();
    }
}
