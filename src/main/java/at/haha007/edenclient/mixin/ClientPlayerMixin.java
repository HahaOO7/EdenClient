package at.haha007.edenclient.mixin;

import at.haha007.edenclient.EdenClient;
import at.haha007.edenclient.command.CommandManager;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.network.packet.c2s.play.UpdateSignC2SPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static at.haha007.edenclient.EdenClient.copy;
import static at.haha007.edenclient.EdenClient.shouldCopy;

@Mixin(ClientPlayerEntity.class)
public class ClientPlayerMixin {
	@Inject(at = @At("HEAD"),
		method = "openEditSignScreen",
		cancellable = true)
	private void init(SignBlockEntity sign, CallbackInfo info) {
		if (!shouldCopy) return;

		UpdateSignC2SPacket packet = new UpdateSignC2SPacket(sign.getPos(),
			copy[0].substring(0, copy[0].length() - 2),
			copy[1].substring(0, copy[1].length() - 2),
			copy[2].substring(0, copy[2].length() - 2),
			copy[3].substring(0, copy[3].length() - 2));
		MinecraftClient.getInstance().getNetworkHandler().sendPacket(packet);
		info.cancel();
	}

	@Inject(at = @At("HEAD"),
		method = "sendChatMessage",
		cancellable = true)
	void sendMessage(String message, CallbackInfo ci) {
		if (!message.startsWith("/")) return;
		if (CommandManager.onCommand(message)) ci.cancel();

//		String[] split = message.toLowerCase().split(" ");
//		switch (split[0].replaceFirst("/", "")) {
//			case "as":
//			case "autosell":
//				EdenClient.INSTANCE.as.onCommand(split);
//				ci.cancel();
//				break;
//		}
	}


}
