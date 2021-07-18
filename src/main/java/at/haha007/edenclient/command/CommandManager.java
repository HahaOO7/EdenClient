package at.haha007.edenclient.command;

import at.haha007.edenclient.utils.PlayerUtils;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.client.network.ClientCommandSource;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class CommandManager {
    private static final List<LiteralArgumentBuilder<ClientCommandSource>> cmds = new ArrayList<>();
    private static final CommandDispatcher<ClientCommandSource> dispatcher = new CommandDispatcher<>();

    static {
        register(literal("cmds").executes(a -> {
            PlayerUtils.sendModMessage(
                    new LiteralText(cmds.
                            stream().
                            map(LiteralArgumentBuilder::getLiteral).
                            collect(Collectors.joining(", "))).
                            formatted(Formatting.GOLD));
            return 1;
        }));
    }

    public static void register(CommandDispatcher<ClientCommandSource> dispatcher) {
        cmds.forEach(dispatcher::register);
    }

    public static void register(LiteralArgumentBuilder<ClientCommandSource> command) {
        cmds.add(command);
        cmds.sort(Comparator.comparing(LiteralArgumentBuilder::getLiteral));
        dispatcher.register(command);
    }

    public static LiteralArgumentBuilder<ClientCommandSource> literal(String s) {
        return LiteralArgumentBuilder.literal(s);
    }

    public static <T> RequiredArgumentBuilder<ClientCommandSource, T> argument(String name, ArgumentType<T> type) {
        return RequiredArgumentBuilder.argument(name, type);
    }

    public static boolean isClientSideCommand(String command) {
        return cmds.stream().anyMatch(c -> c.getLiteral().equalsIgnoreCase(command));
    }


    public static void execute(String command, ClientCommandSource clientCommandSource) {
        try {
            dispatcher.execute(command, clientCommandSource);
        } catch (CommandSyntaxException e) {
            e.printStackTrace();
        }
    }
}
