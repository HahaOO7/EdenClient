package at.haha007.edenclient.mods;

import at.haha007.edenclient.utils.PlayerUtils;
import com.mojang.brigadier.arguments.StringArgumentType;

import static at.haha007.edenclient.command.CommandManager.*;

//A text with click action can only perform one command, this way it can perform multiple.
public class MultiCommand {
    public MultiCommand() {
        register(literal("multicommand").then(argument("commands", StringArgumentType.greedyString())
                        .executes(c -> {
                            String cmds = c.getArgument("commands", String.class);
                            String[] split = cmds.split("%\\|%");
                            for (String s : split) {
                                PlayerUtils.messageC2S("/" + s.trim());
                            }
                            return 1;
                        }))
                .executes(c -> {
                    PlayerUtils.sendModMessage("Command for internal use!");
                    PlayerUtils.sendModMessage("/multicommand command1 %|% command2");
                    return 1;
                }));
    }
}
