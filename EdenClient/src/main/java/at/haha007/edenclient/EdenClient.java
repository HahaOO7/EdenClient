package at.haha007.edenclient;

import at.haha007.edenclient.mods.*;
import at.haha007.edenclient.mods.datafetcher.DataFetcher;
import at.haha007.edenclient.utils.ModInitializer;
import at.haha007.edenclient.utils.Scheduler;
import at.haha007.edenclient.utils.config.PerWorldConfig;
import lombok.Getter;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.Minecraft;

import java.io.File;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EdenClient implements ClientModInitializer {
    private static final ModInitializer modInitializer = new ModInitializer();
    @Getter
    private static EdenClient instance;
    public static final ExecutorService chatThread = Executors.newSingleThreadExecutor();

    public static void setInstance(EdenClient instance) {
        EdenClient.instance = instance;
    }

    @Override
    public void onInitializeClient() {
        setInstance(this);
        PerWorldConfig.get();
        modInitializer.initializeMods();
        //For limited access EdenClient use:
//        modInitializer.initializeMods(List.of(
//                Scheduler.class,
//                EnsureSilk.class,
//                WordHighlighter.class,
//                Rainbowifier.class,
//                ContainerDisplay.class,
//                AntiSpam.class,
//                MessageIgnorer.class,
//                DataFetcher.class));
    }

    public static <T> T getMod(Class<T> clazz) {
        return modInitializer.getMod(clazz);
    }

    public static File getDataFolder() {
        File file = getConfigDirectory();
        if (!file.exists())
            //noinspection ResultOfMethodCallIgnored
            file.mkdirs();
        return file;
    }

    public static File getConfigDirectory() {
        return new File(Minecraft.getInstance().gameDirectory, "config");
    }

}
