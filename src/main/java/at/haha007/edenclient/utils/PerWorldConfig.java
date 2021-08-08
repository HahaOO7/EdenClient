package at.haha007.edenclient.utils;

import at.haha007.edenclient.EdenClient;
import at.haha007.edenclient.callbacks.ConfigLoadCallback;
import at.haha007.edenclient.callbacks.ConfigSaveCallback;
import at.haha007.edenclient.callbacks.JoinWorldCallback;
import at.haha007.edenclient.callbacks.LeaveWorldCallback;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;

import java.io.File;
import java.io.IOException;

public class PerWorldConfig {

    private static PerWorldConfig INSTANCE;
    private NbtCompound tag = new NbtCompound();
    private String worldName = "null";
    private final File folder;

    public static void getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new PerWorldConfig();
        }
    }

    private PerWorldConfig() {
        folder = new File(EdenClient.getDataFolder(), "PerWorldCfg");
        JoinWorldCallback.EVENT.register(this::onJoin);
        LeaveWorldCallback.EVENT.register(this::onLeave);
    }

    private void onLeave(ClientWorld world) {
        System.out.println("leave world: " + worldName);
        saveConfig();
    }

    private void onJoin(ClientWorld world) {
        worldName = StringUtils.getWorldOrServerName();
        System.out.println("join world: " + worldName);
        loadConfig();
    }

    private void loadConfig() {
        File file = new File(folder, worldName + ".mca");
        if (!folder.exists()) folder.mkdirs();
        try {
            tag = file.exists() ? NbtIo.readCompressed(file) : new NbtCompound();
        } catch (IOException e) {
            System.err.println("Error while loading PerWorldConfig: " + worldName);
            tag = new NbtCompound();
        }
        ConfigLoadCallback.EVENT.invoker().onLoad(tag);
    }

    private void saveConfig() {
        ConfigSaveCallback.EVENT.invoker().onSave(tag);
        File file = new File(folder, worldName + ".mca");
        if (!folder.exists()) folder.mkdirs();
        try {
            NbtIo.writeCompressed(tag, file);
        } catch (IOException e) {
            System.err.println("Error while saving PerWorldConfig: " + worldName);
        }
    }
}
