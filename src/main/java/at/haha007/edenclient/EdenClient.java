package at.haha007.edenclient;

import at.haha007.edenclient.mods.*;
import at.haha007.edenclient.mods.chestshop.ChestShopMod;
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

        // Chat | These Mods interact with each message being sent to the client (in descending order)
        new SellStatsTracker();
        new ChestShopMod();
        new MessageIgnorer();
        new WordHighlighter();
        new AntiSpam();

        // Gameplay | These Mods interact with your gameplay passively
        new AutoSell();
        new BarrierDisplay();
        new ItemEsp();
        new AntiStrip();
        new AutoSheer();
        new SignCopy();
        new Nuker();
        
        // Commands only | These Mods only actively interact with your gameplay when directly using its commands
        new Rainbowifier();
        new NbtInfo();
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
