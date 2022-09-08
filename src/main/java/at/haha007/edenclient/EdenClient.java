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
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EdenClient implements ClientModInitializer {
    public static EdenClient INSTANCE;
    private static final Map<Class<?>, Object> registeredMods = new HashMap<>();
    public static ExecutorService chatThread = Executors.newSingleThreadExecutor();;

    @Override
    public void onInitializeClient() {
        INSTANCE = this;

        PerWorldConfig.get();
        Scheduler.get();

        registerMod(DataFetcher.class);
        registerMod(CubeRenderer.class);
        registerMod(TracerRenderer.class);

        // Chat | These Mods interact with each message being sent to the client (in descending order)
        registerMod(SellStatsTracker.class);
        registerMod(ChestShopMod.class);
        registerMod(MessageIgnorer.class);
        registerMod(WordHighlighter.class);
        registerMod(Greetings.class);
        registerMod(AntiSpam.class);

        // Gameplay | These Mods interact with your gameplay passively
        registerMod(AutoSell.class);
        registerMod(BarrierDisplay.class);
        registerMod(ItemEsp.class);
        registerMod(EntityEsp.class);
        registerMod(TileEntityEsp.class);
        registerMod(AntiStrip.class);
        registerMod(AutoSheer.class);
        registerMod(SignCopy.class);
        registerMod(Nuker.class);
        registerMod(AutoHarvest.class);
        registerMod(LifeSaver.class);
        registerMod(GetTo.class);
        registerMod(AntiAfk.class);
        registerMod(ContainerDisplay.class);
        registerMod(HeadHunt.class);
        registerMod(AutoMoss.class);
        registerMod(DoubleDoor.class);

        // Commands only | These Mods only actively interact with your gameplay when directly using its commands
        registerMod(Rainbowifier.class);
        registerMod(NbtInfo.class);
        registerMod(WorldEditReplaceHelper.class);
        registerMod(RenderShape.class);
    }

    private void registerMod(Class<?> clazz) {
        try {
            Constructor<?> constructor = clazz.getConstructor();
            constructor.setAccessible(true);
            Object object = constructor.newInstance();
            registeredMods.put(object.getClass(), object);
        } catch (InstantiationException |
                 IllegalAccessException |
                 InvocationTargetException |
                 NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    public static <T> T getMod(Class<T> clazz) {
        //noinspection unchecked
        return (T) registeredMods.get(clazz);
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
