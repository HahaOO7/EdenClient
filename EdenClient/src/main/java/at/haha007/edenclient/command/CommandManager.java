package at.haha007.edenclient.command;

import at.haha007.edenclient.utils.ChatColor;
import at.haha007.edenclient.utils.PlayerUtils;
import at.haha007.edenclient.utils.Utils;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.network.chat.*;
import net.minecraft.network.chat.contents.PlainTextContents;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class CommandManager {
    private static final Map<LiteralArgumentBuilder<ClientSuggestionProvider>, MutableComponent[]> cmds = new HashMap<>();
    private static final CommandDispatcher<ClientSuggestionProvider> dispatcher = new CommandDispatcher<>();

    static {
        registerCommand("ecmds");
        registerCommand("ehelp");
    }

    private static void registerCommand(String literal) {
        LiteralArgumentBuilder<ClientSuggestionProvider> node = literal(literal);

        node.executes(a -> {
            PlayerUtils.sendModMessage(Component.literal(ChatColor.GOLD + "Click on the mod you need help for to receive help. To get all information for each feature use the github-wiki: ")
                    .append(Component.literal(ChatColor.AQUA + "https://github.com/HahaOO7/EdenClient/wiki")
                            .setStyle(Style.EMPTY
                                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.nullToEmpty("Click to copy the link to the wiki.")))
                                    .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, "https://github.com/HahaOO7/EdenClient/wiki"))
                            )));

            MutableComponent text = Component.literal("");
            Iterator<MutableComponent> it = cmds.keySet().
                    stream().
                    map(LiteralArgumentBuilder::getLiteral).
                    map(Component::literal).
                    map(t -> t.withStyle(ChatFormatting.GOLD)
                            .withStyle(s -> s.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.nullToEmpty("Click for more info."))))
                            .withStyle(s -> s.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/" + node.getLiteral() + " " + ((PlainTextContents.LiteralContents) t.getContents()).text())))).
                    sorted(Comparator.comparing(Object::toString)).
                    toList().iterator();

            while (it.hasNext()) {
                text.append(it.next().withStyle(ChatFormatting.GOLD));
                if (it.hasNext()) {
                    text.append(Component.literal(", ").withStyle(ChatFormatting.AQUA));
                }
            }

            PlayerUtils.sendModMessage(text);
            return 1;
        });

        node.then(argument("cmd", StringArgumentType.word())
                .suggests(CommandManager::suggestCommands)
                .executes(CommandManager::sendCommandHelp));

        register(node,
                "Help for EdenClient command",
                "/ecmds <command> or /ehelp <command>");
    }

    private static int sendCommandHelp(CommandContext<ClientSuggestionProvider> c) {
        String cmdName = c.getArgument("cmd", String.class);
        cmds.entrySet().stream()
                .filter(e -> cmdName.equalsIgnoreCase(e.getKey().getLiteral()))
                .findFirst()
                .map(Map.Entry::getValue)
                .ifPresentOrElse(a -> Arrays.stream(a).forEach(PlayerUtils::sendModMessage),
                        () -> PlayerUtils.sendModMessage("Could not find help for this command"));
        return 0;
    }

    private static CompletableFuture<Suggestions> suggestCommands(CommandContext<ClientSuggestionProvider> c, SuggestionsBuilder b) {
        cmds.keySet().forEach(cmd -> b.suggest(cmd.getLiteral()));
        return b.buildFuture();
    }

    public static void register(CommandDispatcher<ClientSuggestionProvider> dispatcher) {
        cmds.keySet().forEach(dispatcher::register);
    }

    public static void register(LiteralArgumentBuilder<ClientSuggestionProvider> command, MutableComponent... usage) {
        cmds.put(command, usage);
        dispatcher.register(command);
    }

    public static void register(LiteralArgumentBuilder<ClientSuggestionProvider> command, String... usage) {
        MutableComponent[] usg = usage == null ? null : Arrays.stream(usage).map(ChatColor::translateColors).toArray(MutableComponent[]::new);
        register(command, usg);
    }

    public static void register(LiteralArgumentBuilder<ClientSuggestionProvider> command) {
        register(command, (MutableComponent[]) null);
    }

    public static LiteralArgumentBuilder<ClientSuggestionProvider> literal(String s) {
        return LiteralArgumentBuilder.literal(s);
    }

    public static <T> RequiredArgumentBuilder<ClientSuggestionProvider, T> argument(String name, ArgumentType<T> type) {
        return RequiredArgumentBuilder.argument(name, type);
    }

    public static boolean isClientSideCommand(String command) {
        return cmds.keySet().stream().anyMatch(c -> c.getLiteral().equalsIgnoreCase(command));
    }


    public static void execute(String command, ClientSuggestionProvider clientCommandSource) {
        try {
            dispatcher.execute(command, clientCommandSource);
        } catch (CommandSyntaxException e) {
            Utils.getLogger().error(e.getMessage(), e);
        }
    }
}
