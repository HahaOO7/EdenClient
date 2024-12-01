package at.haha007.edenclient.command;

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
import com.mojang.logging.LogUtils;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

public class CommandManager {
    private CommandManager() {
        throw new IllegalStateException("Utility class");
    }

    private static final Map<LiteralArgumentBuilder<FabricClientCommandSource>, Component[]> cmds = new HashMap<>();
    private static CommandDispatcher<FabricClientCommandSource> dispatcher = new CommandDispatcher<>();

    static {
        reset();
    }

    private static void registerCommand(String literal) {
        LiteralArgumentBuilder<FabricClientCommandSource> node = literal(literal);

        node.executes(a -> {
            PlayerUtils.sendModMessage(Component.text("Click on the mod you need help for to receive help. To get all information for each feature use the github-wiki: ", NamedTextColor.GOLD)
                    .append(Component.text("https://github.com/HahaOO7/EdenClient/wiki")
                            .style(Style.style(NamedTextColor.AQUA)
                                    .hoverEvent(HoverEvent.hoverEvent(HoverEvent.Action.SHOW_TEXT, Component.text("Click to copy the link to the wiki.")))
                                    .clickEvent(ClickEvent.clickEvent(ClickEvent.Action.OPEN_URL, "https://github.com/HahaOO7/EdenClient/wiki")))));

            Component text = Component.text("");
            Iterator<Component> it = cmds.keySet().stream()
                    .map(LiteralArgumentBuilder::getLiteral)
                    .map(Component::text)
                    .map(t -> t.style(Style.style(NamedTextColor.GOLD)
                            .hoverEvent(HoverEvent.hoverEvent(HoverEvent.Action.SHOW_TEXT, Component.text("Click for more info.")))
                            .clickEvent(ClickEvent.clickEvent(ClickEvent.Action.RUN_COMMAND, "/" + node.getLiteral() + " " + t.content()))))
                    .sorted(Comparator.comparing(Object::toString))
                    .map(Component.class::cast)
                    .toList().iterator();

            while (it.hasNext()) {
                text = text.append(it.next().color(NamedTextColor.GOLD));
                if (it.hasNext()) {
                    text = text.append(Component.text(", ", NamedTextColor.AQUA));
                }
            }

            PlayerUtils.sendModMessage(text);
            return 1;
        });

        node.then(argument("cmd", StringArgumentType.word()).suggests(CommandManager::suggestCommands).executes(CommandManager::sendCommandHelp));

        register(node, "Help for EdenClient command", "/ecmds <command> or /ehelp <command>");
    }

    private static int sendCommandHelp(CommandContext<FabricClientCommandSource> c) {
        String cmdName = c.getArgument("cmd", String.class);
        cmds.entrySet().stream()
                .filter(e -> cmdName.equalsIgnoreCase(e.getKey().getLiteral()))
                .findFirst()
                .map(Map.Entry::getValue)
                .ifPresentOrElse(a -> Arrays.stream(a)
                                .forEach(PlayerUtils::sendModMessage),
                        () -> PlayerUtils.sendModMessage("Could not find help for this command"));
        return 0;
    }

    private static CompletableFuture<Suggestions> suggestCommands(CommandContext<FabricClientCommandSource> c, SuggestionsBuilder b) {
        cmds.keySet().forEach(cmd -> b.suggest(cmd.getLiteral()));
        return b.buildFuture();
    }

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        cmds.keySet().forEach(dispatcher::register);
    }

    public static void register(LiteralArgumentBuilder<FabricClientCommandSource> command, Component... usage) {
        cmds.put(command, usage);
        dispatcher.register(command);
    }

    public static void reset() {
        dispatcher = new CommandDispatcher<>();
        registerCommand("ecmds");
        registerCommand("ehelp");
        cmds.clear();
    }

    public static void register(LiteralArgumentBuilder<FabricClientCommandSource> command, String... usage) {
        Component[] usg = usage == null ? null : Arrays.stream(usage).map(Component::text).toArray(Component[]::new);
        register(command, usg);
    }

    public static void register(LiteralArgumentBuilder<FabricClientCommandSource> command) {
        register(command, (Component) null);
    }

    public static LiteralArgumentBuilder<FabricClientCommandSource> literal(String name) {
        return LiteralArgumentBuilder.literal(name);
    }

    public static RequiredArgumentBuilder<FabricClientCommandSource, String> fakeLiteral(String name, Predicate<CommandContext<FabricClientCommandSource>> condition) {
        return RequiredArgumentBuilder.<FabricClientCommandSource, String>argument(name, StringArgumentType.word()).suggests((c, b) -> {
            if (condition.test(c))
                b.suggest(name);
            return b.buildFuture();
        });
    }

    public static <T> RequiredArgumentBuilder<FabricClientCommandSource, T> argument(String name, ArgumentType<T> type) {
        return RequiredArgumentBuilder.argument(name, type);
    }

    public static boolean isClientSideCommand(String command) {
        return cmds.keySet().stream().anyMatch(c -> c.getLiteral().equalsIgnoreCase(command));
    }

    public static void execute(String command, FabricClientCommandSource clientCommandSource) {
        try {
            dispatcher.execute(command, clientCommandSource);
        } catch (CommandSyntaxException e) {
            PlayerUtils.sendModMessage(e.getMessage());
            LogUtils.getLogger().error(e.getMessage(), e);
        }
    }
}
