package at.haha007.edenclient;

import at.haha007.edenclient.utils.ModInitializer;
import at.haha007.edenclient.utils.Scheduler;
import at.haha007.edenclient.utils.config.PerWorldConfig;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.Minecraft;

import java.io.File;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EdenClient implements ClientModInitializer {
    private static final ModInitializer modInitializer = new ModInitializer();
    public static EdenClient INSTANCE;
    public static ExecutorService chatThread = Executors.newSingleThreadExecutor();

    @Override
    public void onInitializeClient() {
        INSTANCE = this;
        PerWorldConfig.get();
        modInitializer.initializeMods();
//        modInitializer.initializeMods(List.of(Scheduler.class));
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
