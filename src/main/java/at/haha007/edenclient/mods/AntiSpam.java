package at.haha007.edenclient.mods;

import at.haha007.edenclient.callbacks.AddChatMessageCallback;
import at.haha007.edenclient.utils.ChatColor;
import at.haha007.edenclient.utils.MathUtils;
import at.haha007.edenclient.utils.config.ConfigSubscriber;
import at.haha007.edenclient.utils.config.PerWorldConfig;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.client.GuiMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.components.ComponentRenderUtils;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.FormattedCharSink;
import net.minecraft.util.Mth;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import static at.haha007.edenclient.command.CommandManager.literal;
import static at.haha007.edenclient.command.CommandManager.register;
import static at.haha007.edenclient.utils.PlayerUtils.sendModMessage;

public class AntiSpam {
    @ConfigSubscriber("false")
    private boolean enabled;

    public AntiSpam() {
        registerCommand();
        AddChatMessageCallback.EVENT.register(this::onChat);
        PerWorldConfig.get().register(this, "antiSpam");
    }

    private void registerCommand() {
        LiteralArgumentBuilder<ClientSuggestionProvider> node = literal("eantispam");
        node.then(literal("toggle").executes(c -> {
            enabled = !enabled;
            sendModMessage(ChatColor.GOLD + (enabled ? "Antispam enabled" : "Antispam disabled"));
            return 1;
        }));

        register(node,
                "AntiSpam compresses the chat to eliminate all types of unnecessary spam.",
                "The newest instance of the message will still be displayed, all older ones removed. The number in the square brackets shows how often a message was repeated.");
    }

    private void onChat(AddChatMessageCallback.ChatAddEvent event) {
        if (!enabled) return;
        List<GuiMessage.Line> chatLines = event.getChatLines();
        Component chatText = event.getChatText();
        if (chatText == null) return;
        if (chatLines.isEmpty())
            return;

        class JustGiveMeTheStringVisitor implements FormattedCharSink {
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

        ChatComponent chat = Minecraft.getInstance().gui.getChat();
        int maxTextLength =
                Mth.floor(chat.getWidth() / chat.getScale());
        List<FormattedCharSequence> newLines = ComponentRenderUtils.wrapComponents(
                chatText, maxTextLength, Minecraft.getInstance().font);

        int spamCounter = 1;
        int matchingLines = 0;

        for (int i = chatLines.size() - 1; i >= 0; i--) {
            JustGiveMeTheStringVisitor oldLineVS =
                    new JustGiveMeTheStringVisitor();
            chatLines.get(i).content().accept(oldLineVS);
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
                    chatLines.get(i - 1).content().accept(nextOldLineVS);
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

        chatText = Component.literal("").append(chatText).append(Component.literal(" [x" + spamCounter + "]")
                .setStyle(Style.EMPTY.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(new SimpleDateFormat("hh:mm:ss").format(new Date()))))));

        event.setChatText(chatText);
    }
}
