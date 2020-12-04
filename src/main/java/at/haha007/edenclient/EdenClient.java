package at.haha007.edenclient;

import at.haha007.edenclient.command.Command;
import at.haha007.edenclient.command.CommandManager;
import at.haha007.edenclient.mods.AntiSpam;
import at.haha007.edenclient.mods.AutoSell;
import at.haha007.edenclient.mods.CheshShop.ChestShopMod;
import at.haha007.edenclient.mods.NbtInfo;
import at.haha007.edenclient.mods.SignCopy;
import fi.dy.masa.malilib.util.FileUtils;
import net.fabricmc.api.ClientModInitializer;

import java.io.File;

public class EdenClient implements ClientModInitializer {
	public static EdenClient INSTANCE;
	public AutoSell autoSell = new AutoSell();
	public AntiSpam antiSpam = new AntiSpam();
	public ChestShopMod chestShopMod = new ChestShopMod();
	public NbtInfo nbtInfo = new NbtInfo();

	public static File getDataFolder() {
		File file = FileUtils.getConfigDirectory();
		assert file.exists() || file.mkdirs();
		return file;
	}

	@Override
	public void onInitializeClient() {
		INSTANCE = this;
		new SignCopy();
		CommandManager.registerCommand(new Command(autoSell::onCommand), "autosell", "as");
		CommandManager.registerCommand(new Command(antiSpam::onCommand), "antispam");
		CommandManager.registerCommand(new Command(chestShopMod::onCommand), "chestshop", "cs");
		CommandManager.registerCommand(new Command(CommandManager::onCommand), "commands", "cmds");
		CommandManager.registerCommand(new Command(nbtInfo::onCommand), "nbtinfo", "nbt");
	}

}
