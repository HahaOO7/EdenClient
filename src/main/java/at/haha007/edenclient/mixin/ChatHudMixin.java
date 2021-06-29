package at.haha007.edenclient.mixin;

import at.haha007.edenclient.callbacks.AddChatMessageCallback;
import at.haha007.edenclient.mods.AntiSpam;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;


@Mixin(ChatHud.class)
public abstract class ChatHudMixin extends DrawableHelper {

	@Final
	@Shadow
	private List<ChatHudLine<OrderedText>> visibleMessages;

	@Shadow
	protected abstract void addMessage(Text stringRenderable, int messageId, int timestamp, boolean bl);

	@Inject(at = @At("HEAD"),
			method = "addMessage(Lnet/minecraft/text/Text;I)V",
			cancellable = true)
	private void onAddMessage(Text chatText, int chatLineId, CallbackInfo ci) {
		ActionResult result = AddChatMessageCallback.EVENT.invoker().interact(MinecraftClient.getInstance().player, chatText, chatLineId, visibleMessages);
		if (result == ActionResult.FAIL) ci.cancel();
		AntiSpam.messagesToAdd.forEach(message -> addMessage(message, chatLineId, MinecraftClient.getInstance().inGameHud.getTicks(), false));
		AntiSpam.messagesToAdd.clear();
	}
}
