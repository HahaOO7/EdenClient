package at.haha007.edenclient.mixin;

import at.haha007.edenclient.utils.MathUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.client.util.ChatMessages;
import net.minecraft.text.*;
import net.minecraft.util.math.MathHelper;
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
		MinecraftClient MC = MinecraftClient.getInstance();

		List<ChatHudLine<OrderedText>> chatLines = visibleMessages;
		if (chatLines.isEmpty())
			return;

		class JustGiveMeTheStringVisitor implements CharacterVisitor {
			final StringBuilder sb = new StringBuilder();

			@Override
			public boolean accept(int index, Style style, int codePoint) {
				sb.appendCodePoint(codePoint);
				return true;
			}

			@Override
			public String toString() {
				return sb.toString();
			}
		}

		ChatHud chat = MC.inGameHud.getChatHud();
		int maxTextLength =
			MathHelper.floor(chat.getWidth() / chat.getChatScale());
		List<OrderedText> newLines = ChatMessages.breakRenderedChatMessageLines(
			chatText, maxTextLength, MC.textRenderer);

		int spamCounter = 1;
		int matchingLines = 0;

		for (int i = chatLines.size() - 1; i >= 0; i--) {
			JustGiveMeTheStringVisitor oldLineVS =
				new JustGiveMeTheStringVisitor();
			chatLines.get(i).getText().accept(oldLineVS);
			String oldLine = oldLineVS.toString();

			if (matchingLines <= newLines.size() - 1) {
				JustGiveMeTheStringVisitor newLineVS =
					new JustGiveMeTheStringVisitor();
				newLines.get(matchingLines).accept(newLineVS);
				String newLine = newLineVS.toString();

				if (matchingLines < newLines.size() - 1) {
					if (oldLine.equals(newLine))
						matchingLines++;
					else
						matchingLines = 0;

					continue;
				}

				if (!oldLine.startsWith(newLine)) {
					matchingLines = 0;
					continue;
				}

				if (i > 0 && matchingLines == newLines.size() - 1) {
					JustGiveMeTheStringVisitor nextOldLineVS =
						new JustGiveMeTheStringVisitor();
					chatLines.get(i - 1).getText().accept(nextOldLineVS);
					String nextOldLine = nextOldLineVS.toString();

					String twoLines = oldLine + nextOldLine;
					String addedText = twoLines.substring(newLine.length());

					if (addedText.startsWith(" [x") && addedText.endsWith("]")) {
						String oldSpamCounter =
							addedText.substring(3, addedText.length() - 1);

						if (MathUtils.isInteger(oldSpamCounter)) {
							spamCounter += Integer.parseInt(oldSpamCounter);
							matchingLines++;
							continue;
						}
					}
				}

				if (oldLine.length() == newLine.length())
					spamCounter++;
				else {
					String addedText = oldLine.substring(newLine.length());
					if (!addedText.startsWith(" [x") || !addedText.endsWith("]")) {
						matchingLines = 0;
						continue;
					}

					String oldSpamCounter =
						addedText.substring(3, addedText.length() - 1);
					if (!MathUtils.isInteger(oldSpamCounter)) {
						matchingLines = 0;
						continue;
					}

					spamCounter += Integer.parseInt(oldSpamCounter);
				}
			}

			if (i + matchingLines >= i) {
				chatLines.subList(i, i + matchingLines + 1).clear();
			}
			matchingLines = 0;
		}

		if (spamCounter > 1)
			chatText = new LiteralText(chatText.getString() + " [x" + spamCounter + "]");

		addMessage(chatText, chatLineId, MC.inGameHud.getTicks(), false);
		ci.cancel();
	}
}
