package at.haha007.edenclient;

import at.haha007.edenclient.command.Command;
import at.haha007.edenclient.command.CommandManager;
import at.haha007.edenclient.mods.*;
import at.haha007.edenclient.mods.chestshop.ChestShopMod;
import at.haha007.edenclient.utils.PerWorldConfig;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.MinecraftClient;

import java.io.File;
import java.net.SocketAddress;

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
        File file = getConfigDirectory();
        if (!file.exists()) file.mkdirs();
        return file;
    }

    public static File getConfigDirectory() {
        return new File(MinecraftClient.getInstance().runDirectory, "config");
    }


}
