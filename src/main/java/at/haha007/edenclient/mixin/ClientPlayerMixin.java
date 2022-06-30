package at.haha007.edenclient.mixin;

import at.haha007.edenclient.callbacks.PlayerEditSignCallback;
import at.haha007.edenclient.callbacks.PlayerTickCallback;
import at.haha007.edenclient.command.CommandManager;
import at.haha007.edenclient.utils.PlayerUtils;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientCommandSource;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(ClientPlayerEntity.class)
public abstract class ClientPlayerMixin {

    @Shadow
    @Final
    public ClientPlayNetworkHandler networkHandler;

    @Shadow
    @Final
    protected MinecraftClient client;

    @Inject(at = @At("HEAD"), method = "openEditSignScreen", cancellable = true)
    private void onEditSign(SignBlockEntity sign, CallbackInfo info) {
        ActionResult result = PlayerEditSignCallback.EVENT.invoker().interact(PlayerUtils.getPlayer(), sign);
        if (result == ActionResult.FAIL) info.cancel();
    }

    @Inject(at = @At("HEAD"), method = "tick")
    void tickMovement(CallbackInfo ci) {
        PlayerTickCallback.EVENT.invoker().interact(PlayerUtils.getPlayer());
    }

    @Inject(at = @At("HEAD"), method = "sendCommand(Ljava/lang/String;Lnet/minecraft/text/Text;)V", cancellable = true)
    void sendCommand(String message, Text preview, CallbackInfo ci) {
        if (!CommandManager.isClientSideCommand(message.split(" ")[0]))
            return;
        CommandManager.execute(message, new ClientCommandSource(networkHandler, client));
        ci.cancel();
    }
}
