package at.haha007.edenclient;

import at.haha007.edenclient.command.Command;
import at.haha007.edenclient.command.CommandManager;
import at.haha007.edenclient.mods.*;
import at.haha007.edenclient.mods.chestshop.ChestShopMod;
import at.haha007.edenclient.mods.WordHighlighter;
import at.haha007.edenclient.utils.PerWorldConfig;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.MinecraftClient;

import java.io.File;

public class EdenClient implements ClientModInitializer {
    public static EdenClient INSTANCE;

    @Override
    public void onInitializeClient() {
        INSTANCE = this;
        PerWorldConfig.getInstance();
        new SignCopy();
        new BarrierDisplay();
        new AutoSell();
        new ItemEsp();
        new AntiSpam();
        new NbtInfo();
        new ChestShopMod();
        new WordHighlighter();
        new AntiStrip();

        CommandManager.registerCommand(new Command(CommandManager::onCommand), "commands", "cmds");
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
