package at.haha007.edenclient;

import at.haha007.edenclient.annotations.Mod;
import at.haha007.edenclient.callbacks.AddChatMessageCallback;
import at.haha007.edenclient.callbacks.AddChatMessageCallback.ChatAddEvent;
import at.haha007.edenclient.callbacks.Event;
import at.haha007.edenclient.callbacks.GameRenderCallback;
import at.haha007.edenclient.mixinterface.ChatComponentAccessor;
import at.haha007.edenclient.utils.ModInitializer;
import at.haha007.edenclient.utils.PlayerUtils;
import at.haha007.edenclient.utils.config.PerWorldConfig;
import fi.dy.masa.malilib.event.RenderEventHandler;
import fi.dy.masa.malilib.interfaces.IRenderer;
import lombok.Getter;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.GuiMessageTag;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import java.io.File;
import java.util.LinkedList;
import java.util.Queue;
import java.util.function.Predicate;

public class EdenClient implements ClientModInitializer {
    private static final ModInitializer modInitializer = new ModInitializer();
    @Getter
    private static EdenClient instance;
    public static boolean connected = false;
    public static final Queue<Component> chatMessagesToHandle = new LinkedList<>();

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

                Component component = chatMessagesToHandle.poll();
                if (component == null) return;
                ChatComponentAccessor chat = (ChatComponentAccessor) Minecraft.getInstance().gui.getChat();
                LocalPlayer player = PlayerUtils.getPlayer();
                ChatAddEvent event = new ChatAddEvent(player, component, chat.edenClient$getTrimmedMessages());
                AddChatMessageCallback.EVENT.invoker().onChatAdd(event);
                Component chatText = event.getChatText();
                if (chatText != null && !chatText.getString().isBlank() && !event.isCanceled()) {
                    Minecraft.getInstance().gui.getChat().addMessage(chatText, null, GuiMessageTag.system());
                }
            }
        });
    }

    public static void onJoin() {
        modInitializer.initializeMods();
        //For limited access EdenClient use:
//        modInitializer.initializeMods(java.util.List.of(
//                at.haha007.edenclient.mods.EnsureSilk.class,
//                at.haha007.edenclient.mods.AntiStrip.class,
//                at.haha007.edenclient.mods.WordHighlighter.class,
//                at.haha007.edenclient.mods.Rainbowifier.class,
//                at.haha007.edenclient.mods.ContainerDisplay.class,
//                at.haha007.edenclient.mods.SignWidthOverride.class,
//                at.haha007.edenclient.mods.NbtInfo.class,
//                at.haha007.edenclient.mods.AntiSpam.class,
//                at.haha007.edenclient.mods.RenderShape.class,
//                at.haha007.edenclient.mods.SearchItem.class,
//                at.haha007.edenclient.mods.MessageIgnorer.class));
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
