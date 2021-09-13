package at.haha007.edenclient;

import at.haha007.edenclient.mods.*;
import at.haha007.edenclient.mods.chestshop.ChestShopMod;
import at.haha007.edenclient.mods.datafetcher.DataFetcher;
import at.haha007.edenclient.render.CubeRenderer;
import at.haha007.edenclient.render.TracerRenderer;
import at.haha007.edenclient.utils.Scheduler;
import at.haha007.edenclient.utils.config.PerWorldConfig;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.MinecraftClient;

import java.io.File;

public class EdenClient implements ClientModInitializer {
    public static EdenClient INSTANCE;
    private MessageIgnorer messageIgnorer;
    private DataFetcher dataFetcher;
    private CubeRenderer cubeRenderer;
    private TracerRenderer tracerRenderer;

    @Override
    public void onInitializeClient() {
        INSTANCE = this;
        PerWorldConfig.get();
        Scheduler.get();

        dataFetcher = new DataFetcher();
        cubeRenderer = new CubeRenderer();
        tracerRenderer = new TracerRenderer();

        // Chat | These Mods interact with each message being sent to the client (in descending order)
        new SellStatsTracker();
        new ChestShopMod();
        messageIgnorer = new MessageIgnorer();
        new WordHighlighter();
        new Greetings();
        new AntiSpam();

        // Gameplay | These Mods interact with your gameplay passively
        new AutoSell();
        new BarrierDisplay();
        new ItemEsp();
        new EntityEsp();
        new TileEntityEsp();
        new AntiStrip();
        new AutoSheer();
        new SignCopy();
        new Nuker();
        new LifeSaver();
        new GetTo();
        new AntiAfk();

        // Commands only | These Mods only actively interact with your gameplay when directly using its commands
        new Rainbowifier();
        new NbtInfo();
        new WorldEditReplaceHelper();
        new RenderShape();
    }

    public MessageIgnorer getMessageIgnorer() {
        return messageIgnorer;
    }

    public DataFetcher getDataFetcher() {
        return dataFetcher;
    }

    public CubeRenderer getCubeRenderer() {
        return cubeRenderer;
    }

    public TracerRenderer getTracerRenderer() {
        return tracerRenderer;
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
