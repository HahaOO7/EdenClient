package at.haha007.edenclient.mixin;

import at.haha007.edenclient.callbacks.PlayerEditSignCallback;
import at.haha007.edenclient.callbacks.PlayerTickCallback;
import at.haha007.edenclient.callbacks.SendChatMessageCallback;
import at.haha007.edenclient.command.CommandManager;
import at.haha007.edenclient.utils.PlayerUtils;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientCommandSource;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
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

    @Inject(at = @At("HEAD"),
            method = "openEditSignScreen",
            cancellable = true)
    private void onEditSign(SignBlockEntity sign, CallbackInfo info) {
        ActionResult result = PlayerEditSignCallback.EVENT.invoker().interact(PlayerUtils.getPlayer(), sign);
        if (result == ActionResult.FAIL) info.cancel();
    }

    @Inject(at = @At("HEAD"),
            method = "tick",
            cancellable = true)
    void tickMovement(CallbackInfo ci) {
        PlayerTickCallback.EVENT.invoker().interact(PlayerUtils.getPlayer());
    }

    @Inject(at = @At("HEAD"),
            method = "sendChatMessage",
            cancellable = true)
    void sendMessage(String message, CallbackInfo ci) {
        SendChatMessageCallback.EVENT.invoker().sendMessage(message);
        if (message.length() < 2 || !message.startsWith("/")) return;
        if (!CommandManager.isClientSideCommand(message.substring(1).split(" ")[0]))
            return;
        CommandManager.execute(message.substring(1), new ClientCommandSource(networkHandler, client));
        ci.cancel();
    }


}
