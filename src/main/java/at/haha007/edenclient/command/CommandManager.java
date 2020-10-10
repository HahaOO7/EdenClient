package at.haha007.edenclient.command;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.LiteralText;

import java.util.Arrays;
import java.util.HashMap;

public class CommandManager {
	private static final HashMap<String, Command> commandMap = new HashMap<>();
	private static final HashMap<String, String[]> aliasMap = new HashMap<>();

	public static void registerCommand(Command command, String name, String... aliases) {
		commandMap.put(name, command);
		for (String alias : aliases) {
			commandMap.put(alias, command);
		}
		aliasMap.put(name, aliases);
	}

	public static boolean onCommand(String command) {
		String[] args = command.replaceFirst("/", "").split(" ");
		Command cmd = commandMap.get(args[0]);
		if (cmd == null)
			return false;
		cmd.getExecutor().executeCommand(cmd, args[0], args.length > 1 ? Arrays.copyOfRange(args, 1, args.length) : new String[0]);
		return true;
	}

	//display all commands
	public static void onCommand(Command command, String label, String[] args) {
		aliasMap.forEach((s, a) -> MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(new LiteralText("Command: " + s + "  Aliases: " + Arrays.toString(a))));
	}
}
