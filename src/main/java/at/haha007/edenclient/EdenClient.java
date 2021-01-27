package at.haha007.edenclient;

import at.haha007.edenclient.command.Command;
import at.haha007.edenclient.command.CommandManager;
import at.haha007.edenclient.mods.*;
import at.haha007.edenclient.mods.chestshop.ChestShopMod;
import fi.dy.masa.malilib.util.FileUtils;
import net.fabricmc.api.ClientModInitializer;

import java.io.File;

public class EdenClient implements ClientModInitializer {
	public static EdenClient INSTANCE;
	public AutoSell autoSell = new AutoSell();
	public AntiSpam antiSpam = new AntiSpam();
	public NbtInfo nbtInfo = new NbtInfo();
	public ChestShopMod chestShopMod = new ChestShopMod();

	@Override
	public void onInitializeClient() {
		INSTANCE = this;
		new SignCopy();
		CommandManager.registerCommand(new Command(autoSell::onCommand), "autosell", "as");
		CommandManager.registerCommand(new Command(antiSpam::onCommand), "antispam");
		CommandManager.registerCommand(new Command(CommandManager::onCommand), "commands", "cmds");
		CommandManager.registerCommand(new Command(nbtInfo::onCommand), "nbtinfo", "nbt");
		CommandManager.registerCommand(new Command(chestShopMod::onCommand), "chestshop", "cs");
		new AntiStrip();
	}


	public static File getDataFolder() {
		File file = FileUtils.getConfigDirectory();
		if (!file.exists()) file.mkdirs();
		return file;
	}
}
