package at.haha007.edenclient.mixin;

import at.haha007.edenclient.callbacks.PlayerTickCallback;
import at.haha007.edenclient.callbacks.PlayerEditSignCallback;
import at.haha007.edenclient.command.CommandManager;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.ActionResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(ClientPlayerEntity.class)
public abstract class ClientPlayerMixin {

	@Inject(at = @At("HEAD"),
		method = "openEditSignScreen",
		cancellable = true)
	private void onEditSign(SignBlockEntity sign, CallbackInfo info) {
		ActionResult result = PlayerEditSignCallback.EVENT.invoker().interact(MinecraftClient.getInstance().player, sign);
		if (result == ActionResult.FAIL) info.cancel();
	}

	@Inject(at = @At("HEAD"),
		method = "tick",
		cancellable = true)
	void tickMovement(CallbackInfo ci) {
		PlayerTickCallback.EVENT.invoker().interact(MinecraftClient.getInstance().player);
	}

	@Inject(at = @At("HEAD"),
		method = "sendChatMessage",
		cancellable = true)
	void sendMessage(String message, CallbackInfo ci) {
		if (!message.startsWith("/")) return;
		if (CommandManager.onCommand(message)) ci.cancel();
	}


}
