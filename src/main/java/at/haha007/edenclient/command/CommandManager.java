package at.haha007.edenclient.command;

import at.haha007.edenclient.utils.ChatColor;
import at.haha007.edenclient.utils.PlayerUtils;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.client.network.ClientCommandSource;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class CommandManager {
    private static final Map<LiteralArgumentBuilder<ClientCommandSource>, Text[]> cmds = new HashMap<>();
    private static final CommandDispatcher<ClientCommandSource> dispatcher = new CommandDispatcher<>();

    static {
        List<LiteralArgumentBuilder<ClientCommandSource>> list = List.of(literal("ecmds"), literal("ehelp"));

        list.forEach(cmd -> cmd.executes(a -> {
            PlayerUtils.sendModMessage(new LiteralText(ChatColor.GOLD + "Click on the mod you need help for to receive help. To get all information for each feature use the github-wiki: ")
                    .append(new LiteralText(ChatColor.AQUA + "https://github.com/HahaOO7/EdenClient/wiki")
                            .setStyle(Style.EMPTY
                                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.of("Click to copy the link to the wiki.")))
                                    .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, "https://github.com/HahaOO7/EdenClient/wiki"))
                            )));

            MutableText text = new LiteralText("");
            Iterator<MutableText> it = cmds.keySet().
                    stream().
                    map(LiteralArgumentBuilder::getLiteral).
                    map(LiteralText::new).
                    map(t -> t.formatted(Formatting.GOLD)
                            .styled(s -> s.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.of("Click for more info."))))
                            .styled(s -> s.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/" + cmd.getLiteral() + " " + t.asString())))).
                    sorted(Comparator.comparing(Object::toString)).
                    collect(Collectors.toList()).iterator();

            while (it.hasNext()) {
                text.append(it.next().formatted(Formatting.GOLD));
                if (it.hasNext()) {
                    text.append(new LiteralText(", ").formatted(Formatting.AQUA));
                }
            }

            PlayerUtils.sendModMessage(text);
            return 1;
        }));

        list.forEach(cmd -> cmd.then(argument("cmd", StringArgumentType.word())
                .suggests(CommandManager::suggestCommands)
                .executes(CommandManager::sendCommandHelp)));

        list.forEach(cmd ->
                register(cmd,
                        "Help for EdenClient commands",
                        "/ecmds <command> or /ehelp <command>"));
    }

    private static int sendCommandHelp(CommandContext<ClientCommandSource> c) {
        String cmdName = c.getArgument("cmd", String.class);
        cmds.entrySet().stream()
                .filter(e -> cmdName.equalsIgnoreCase(e.getKey().getLiteral()))
                .findFirst()
                .map(Map.Entry::getValue)
                .ifPresentOrElse(a -> Arrays.stream(a).forEach(PlayerUtils::sendModMessage),
                        () -> PlayerUtils.sendModMessage("Could not find help for this command"));
        return 0;
    }

    private static CompletableFuture<Suggestions> suggestCommands(CommandContext<ClientCommandSource> c, SuggestionsBuilder b) {
        cmds.keySet().forEach(cmd -> b.suggest(cmd.getLiteral()));
        return b.buildFuture();
    }

    public static void register(CommandDispatcher<ClientCommandSource> dispatcher) {
        cmds.keySet().forEach(dispatcher::register);
    }

    public static void register(LiteralArgumentBuilder<ClientCommandSource> command, String... usage) {
        cmds.put(command, Arrays.stream(usage).map(ChatColor::translateColors).toArray(Text[]::new));
        dispatcher.register(command);
    }

    public static void register(LiteralArgumentBuilder<ClientCommandSource> command) {
        register(command, (String[]) null);
    }

    public static LiteralArgumentBuilder<ClientCommandSource> literal(String s) {
        return LiteralArgumentBuilder.literal(s);
    }

    public static <T> RequiredArgumentBuilder<ClientCommandSource, T> argument(String name, ArgumentType<T> type) {
        return RequiredArgumentBuilder.argument(name, type);
    }

    public static boolean isClientSideCommand(String command) {
        return cmds.keySet().stream().anyMatch(c -> c.getLiteral().equalsIgnoreCase(command));
    }


    public static void execute(String command, ClientCommandSource clientCommandSource) {
        try {
            dispatcher.execute(command, clientCommandSource);
        } catch (CommandSyntaxException e) {
            e.printStackTrace();
        }
    }
}
