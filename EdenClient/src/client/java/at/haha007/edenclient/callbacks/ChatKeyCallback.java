package at.haha007.edenclient.callbacks;


import java.util.List;

public interface ChatKeyCallback {
    Event<ChatKeyCallback> EVENT = new Event<>(
            listeners -> (key, width, signWidth, posInHistory) -> {
                for (ChatKeyCallback listener : listeners) {
                    posInHistory = listener.getNewPosInHistory(key, width, signWidth, posInHistory);
                }
                return posInHistory;
            });

    int getNewPosInHistory(int key, List<String> recent, String prefix, int posInHistory);
}
