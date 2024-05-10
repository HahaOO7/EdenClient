package at.haha007.edenclient.mods;

import at.haha007.edenclient.annotations.Mod;
import at.haha007.edenclient.callbacks.ChatKeyCallback;
import at.haha007.edenclient.utils.config.ConfigSubscriber;
import at.haha007.edenclient.utils.config.PerWorldConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ArrayListDeque;

import java.util.List;

import static at.haha007.edenclient.command.CommandManager.literal;
import static at.haha007.edenclient.command.CommandManager.register;
import static at.haha007.edenclient.utils.PlayerUtils.sendModMessage;

@Mod
public class PageKeyChatCompletion {

    public PageKeyChatCompletion() {
        ChatKeyCallback.EVENT.register(this::onChatKey);
    }

    private int onChatKey(int key, List<String> history, String prefix, int posInHistory) {
        if (266 == key) {
            if (posInHistory == 0) return posInHistory;
            for (int i = posInHistory - 1; i >= 0; i--) {
                if (!history.get(i).startsWith(prefix))
                    continue;
                return i;
            }
        }

        if (267 == key) {
            if (posInHistory > history.size() - 2) return posInHistory;
            for (int i = posInHistory + 1; i < history.size(); i++) {
                if (!history.get(i).startsWith(prefix))
                    continue;
                return i;
            }
        }

        return posInHistory;
    }
}
