package at.haha007.edenclient;

import at.haha007.edenclient.utils.singleton.SingletonLoader;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.MinecraftClient;

import java.io.File;

public class EdenClient implements ClientModInitializer {
    public static EdenClient INSTANCE;

    @Override
    public void onInitializeClient() {
        INSTANCE = this;
        SingletonLoader.get(EdenClient.class);
    }

    public static File getDataFolder() {
        File file = getConfigDirectory();
        if (!file.exists() && !file.mkdirs())
            return null;
        return file;
    }

    public static File getConfigDirectory() {
        return new File(MinecraftClient.getInstance().runDirectory, "config");
    }
}
