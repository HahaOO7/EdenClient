package at.haha007.edenclient;

import at.haha007.edenclient.command.Command;
import at.haha007.edenclient.command.CommandManager;
import net.fabricmc.api.ClientModInitializer;

public class EdenClient implements ClientModInitializer {
	public static String[] copy = new String[4];
	public static boolean shouldCopy = false;
	public static EdenClient INSTANCE;
	public AutoSell as = new AutoSell();

	@Override
	public void onInitializeClient() {
		INSTANCE = this;
		CommandManager.registerCommand(new Command(as::onCommand), "autosell", "as");
	}
}
