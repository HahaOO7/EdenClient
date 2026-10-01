package at.haha007.edenclient.utils;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

public class BlockArgument implements ArgumentType<Block> {
    @Override
    public Block parse(StringReader reader) throws CommandSyntaxException {
        String read = reader.readString();
        return BuiltInRegistries.BLOCK.getValue(Identifier.parse(read));
    }

    public <S> CompletableFuture<Suggestions> listSuggestions(final CommandContext<S> context, final SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(BuiltInRegistries.BLOCK.keySet().stream().map(Identifier::getPath), builder);
    }

    public Collection<String> getExamples() {
        return BuiltInRegistries.BLOCK.keySet().stream().map(Identifier::getPath).toList();
    }
}
