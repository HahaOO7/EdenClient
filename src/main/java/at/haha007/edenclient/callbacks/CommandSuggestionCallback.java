package at.haha007.edenclient.callbacks;

import com.mojang.brigadier.suggestion.Suggestions;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

public interface CommandSuggestionCallback {
    Event<CommandSuggestionCallback> EVENT = EventFactory.createArrayBacked(CommandSuggestionCallback.class,
            listeners -> (suggestions) -> {
                for (CommandSuggestionCallback listener : listeners) {
                    listener.suggest(suggestions);
                }
            });

    void suggest(Suggestions suggestions);
}
