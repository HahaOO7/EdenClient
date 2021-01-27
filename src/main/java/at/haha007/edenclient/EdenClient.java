package at.haha007.edenclient;

import at.haha007.edenclient.command.Command;
import at.haha007.edenclient.command.CommandManager;
import at.haha007.edenclient.mods.*;
import net.fabricmc.api.ClientModInitializer;

public class EdenClient implements ClientModInitializer {
	public static EdenClient INSTANCE;
	public AutoSell autoSell = new AutoSell();
	public AntiSpam antiSpam = new AntiSpam();
	public NbtInfo nbtInfo = new NbtInfo();


	@Override
	public void onInitializeClient() {
		INSTANCE = this;
		new SignCopy();
		CommandManager.registerCommand(new Command(autoSell::onCommand), "autosell", "as");
		CommandManager.registerCommand(new Command(antiSpam::onCommand), "antispam");
		CommandManager.registerCommand(new Command(CommandManager::onCommand), "commands", "cmds");
		CommandManager.registerCommand(new Command(nbtInfo::onCommand), "nbtinfo", "nbt");
		new AntiStrip();
	}

}
