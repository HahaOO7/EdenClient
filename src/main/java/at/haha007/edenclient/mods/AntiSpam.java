package at.haha007.edenclient.mods;

import at.haha007.edenclient.callbacks.AddChatMessageCallback;
import at.haha007.edenclient.command.CommandManager;
import at.haha007.edenclient.utils.MathUtils;
import at.haha007.edenclient.utils.config.ConfigSubscriber;
import at.haha007.edenclient.utils.config.PerWorldConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.client.util.ChatMessages;
import net.minecraft.text.CharacterVisitor;
import net.minecraft.text.LiteralText;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Style;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;

import java.util.List;

import static at.haha007.edenclient.utils.PlayerUtils.sendModMessage;

public class AntiSpam {
    private final MinecraftClient MC = MinecraftClient.getInstance();
    @ConfigSubscriber("false")
    private boolean enabled;

    public AntiSpam() {
        registerCommand();
        AddChatMessageCallback.EVENT.register(this::onChat);
        PerWorldConfig.get().register(this, "antiSpam");
    }

    private void registerCommand() {
        CommandManager.register(CommandManager.literal("antispam").executes(c -> {
            enabled = !enabled;
            sendModMessage(new LiteralText(enabled ? "Antispam enabled" : "Antispam disabled").formatted(Formatting.GOLD));
            return 1;
        }));
    }

    private void onChat(AddChatMessageCallback.ChatAddEvent event) {
        if (!enabled) return;
        List<ChatHudLine<OrderedText>> chatLines = event.getChatLines();
        var chatText = event.getChatText();
        if (chatText == null) return;
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

        if (spamCounter > 1) {
            chatText = new LiteralText("").append(chatText).append(new LiteralText(" [x" + spamCounter + "]"));
        }

        event.setChatText(chatText);
    }
}
