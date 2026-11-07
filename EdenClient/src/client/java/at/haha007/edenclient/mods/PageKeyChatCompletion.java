package at.haha007.edenclient.mods;

import at.haha007.edenclient.annotations.Mod;
import at.haha007.edenclient.callbacks.ChatKeyCallback;
import com.mojang.blaze3d.platform.InputConstants;

import java.util.List;

@Mod
public class PageKeyChatCompletion {

    public PageKeyChatCompletion() {
        ChatKeyCallback.EVENT.register(this::onChatKey, getClass());
    }

    private int onChatKey(int key, List<String> history, String prefix, int posInHistory) {
        if (InputConstants.KEY_PAGEUP == key) {
            if (posInHistory == 0) return posInHistory;
            for (int i = posInHistory - 1; i >= 0; i--) {
                if (!history.get(i).startsWith(prefix))
                    continue;
                return i;
            }
        }

        if (InputConstants.KEY_PAGEDOWN == key) {
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
