package at.haha007.edenclient;

import at.haha007.edenclient.command.Command;
import at.haha007.edenclient.command.CommandManager;
import at.haha007.edenclient.mods.AutoSell;
import at.haha007.edenclient.mods.SignCopy;
import at.haha007.edenclient.mods.automine.AutoMiner;
import net.fabricmc.api.ClientModInitializer;

public class EdenClient implements ClientModInitializer {
	public static EdenClient INSTANCE;
	public AutoSell autoSell = new AutoSell();
	public AutoMiner autoMiner = new AutoMiner();

	@Override
	public void onInitializeClient() {
		INSTANCE = this;
		CommandManager.registerCommand(new Command(autoSell::onCommand), "autosell", "as");
		CommandManager.registerCommand(new Command(autoMiner::onCommand), "automine", "am");
		new SignCopy();
	}
}
