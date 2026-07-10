package at.haha007.edenclient.callbacks;

import com.mojang.brigadier.suggestion.Suggestions;

public interface CommandSuggestionCallback {
    Event<CommandSuggestionCallback> EVENT = new Event<>(
            listeners -> (suggestions, id) -> {
                for (CommandSuggestionCallback listener : listeners) {
                    listener.suggest(suggestions, id);
                }
            });

    void suggest(Suggestions suggestions, int id);
}
