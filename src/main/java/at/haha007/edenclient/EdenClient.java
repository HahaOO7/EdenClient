package at.haha007.edenclient;

import at.haha007.edenclient.mods.*;
import at.haha007.edenclient.mods.chestshop.ChestShopMod;
import at.haha007.edenclient.mods.datafetcher.DataFetcher;
import at.haha007.edenclient.utils.PerWorldConfig;
import at.haha007.edenclient.utils.Scheduler;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.MinecraftClient;

import java.io.File;

public class EdenClient implements ClientModInitializer {
    public static EdenClient INSTANCE;
    private MessageIgnorer messageIgnorer;
    private DataFetcher dataFetcher;

    @Override
    public void onInitializeClient() {
        INSTANCE = this;
        PerWorldConfig.getInstance();
        Scheduler.get();

        dataFetcher = new DataFetcher();

        // Chat | These Mods interact with each message being sent to the client (in descending order)
        new SellStatsTracker();
        new ChestShopMod();
        messageIgnorer = new MessageIgnorer();
        new WordHighlighter();
        new AntiSpam();

        // Gameplay | These Mods interact with your gameplay passively
        new AutoSell();
        new BarrierDisplay();
        new ItemEsp();
        new EntityEsp();
        new AntiStrip();
        new AutoSheer();
        new SignCopy();
        new Nuker();
        new LifeSaver();

        // Commands only | These Mods only actively interact with your gameplay when directly using its commands
        new Rainbowifier();
        new NbtInfo();
        new WorldEditReplaceHelper();
    }

    public MessageIgnorer getMessageIgnorer() {
        return messageIgnorer;
    }

    public DataFetcher getDataFetcher() {
        return dataFetcher;
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
