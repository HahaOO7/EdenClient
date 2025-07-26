package at.haha007.edenclient;

import at.haha007.edenclient.annotations.Mod;
import at.haha007.edenclient.callbacks.Event;
import at.haha007.edenclient.callbacks.GameRenderCallback;
import at.haha007.edenclient.utils.ModInitializer;
import at.haha007.edenclient.utils.config.PerWorldConfig;
import fi.dy.masa.malilib.event.RenderEventHandler;
import fi.dy.masa.malilib.interfaces.IRenderer;
import lombok.Getter;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.Minecraft;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Predicate;

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
        modInitializer.initializeMods(c -> c.getAnnotation(Mod.class).required());

        RenderEventHandler.getInstance().registerWorldLastRenderer(new IRenderer() {
            @Override
            public void onRenderWorldLast(Matrix4f posMatrix, Matrix4f projMatrix) {
                float partialTick = Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(true);
                GameRenderCallback.EVENT.invoker().render(partialTick);
                GL11.glEnable(GL11.GL_DEPTH_TEST);
            }
        });
    }

    public static void onJoin() {
        modInitializer.initializeMods();
        //For limited access EdenClient use:
//        modInitializer.initializeMods(List.of(
//                EnsureSilk.class,
//                AntiStrip.class,
//                WordHighlighter.class,
//                Rainbowifier.class,
//                ContainerDisplay.class,
//                SignWidthOverride.class,
//                NbtInfo.class,
//                AntiSpam.class,
//                RenderShape.class,
//                MessageIgnorer.class));
    }

    public static void onQuit() {
        Predicate<Class<?>> predicate = c -> c.isAnnotationPresent(Mod.class) &&
                !c.getAnnotation(Mod.class).required();
        Event.unregisterAll(predicate);
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
