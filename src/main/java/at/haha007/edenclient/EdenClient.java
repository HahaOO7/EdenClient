package at.haha007.edenclient;

import at.haha007.edenclient.callbacks.StartGameSessionCallback;
import at.haha007.edenclient.command.Command;
import at.haha007.edenclient.command.CommandManager;
import at.haha007.edenclient.mods.*;
import at.haha007.edenclient.mods.chestshop.ChestShopMod;
import at.haha007.edenclient.utils.PerWorldConfig;
import at.haha007.edenclient.utils.RenderUtils;
import fi.dy.masa.malilib.util.FileUtils;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.Box;
import org.lwjgl.opengl.GL11;

import java.io.File;

public class EdenClient implements ClientModInitializer {
	public static EdenClient INSTANCE;
	public AntiSpam antiSpam = new AntiSpam();
	public NbtInfo nbtInfo = new NbtInfo();
	public ChestShopMod chestShopMod = new ChestShopMod();

	@Override
	public void onInitializeClient() {
		INSTANCE = this;
		PerWorldConfig.getInstance();
		new SignCopy();
		new BarrierDisplay();
		new AutoSell();
		new ItemEsp();
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
