package at.haha007.edenclient.mods;

import at.haha007.edenclient.callbacks.AddChatMessageCallback;
import at.haha007.edenclient.command.Command;
import at.haha007.edenclient.utils.MathUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.util.ChatMessages;
import net.minecraft.text.*;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.MathHelper;

import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class AntiSpam {
	public static final Queue<Text> messagesToAdd = new LinkedList<Text>();
	private final MinecraftClient MC = MinecraftClient.getInstance();
	private boolean enabled = true;

	public AntiSpam() {
		AddChatMessageCallback.EVENT.register(this::onChat);
	}

	private ActionResult onChat(ClientPlayerEntity entity, Text chatText, int chatLineId, List<ChatHudLine<OrderedText>> chatLines) {
		if (!enabled) return ActionResult.PASS;
		if (chatLines.isEmpty())
			return ActionResult.PASS;

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

		if (spamCounter > 1) {
			chatText = new LiteralText("").append(chatText).append(new LiteralText(" [x" + spamCounter + "]"));
		}

		messagesToAdd.add(chatText);
		return ActionResult.FAIL;
	}


	public void onCommand(Command command, String s, String[] strings) {
		if ((enabled = !enabled)) {
			MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(new LiteralText("AntiSpam enabled"));
		} else {
			MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(new LiteralText("AntiSpam disabled"));
		}
	}
}
