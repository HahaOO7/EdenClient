package at.haha007.edenclient.utils;

import at.haha007.edenclient.EdenClient;
import at.haha007.edenclient.callbacks.ConfigLoadCallback;
import at.haha007.edenclient.callbacks.ConfigSaveCallback;
import at.haha007.edenclient.callbacks.LeaveGameSessionCallback;
import at.haha007.edenclient.callbacks.StartGameSessionCallback;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.util.ActionResult;

import java.io.File;
import java.io.IOException;

public class PerWorldConfig {

    private static PerWorldConfig INSTANCE;
    private NbtCompound tag = new NbtCompound();
    private String worldName = "null";
    private final File folder;

    public static PerWorldConfig getInstance() {
        return INSTANCE == null ? (INSTANCE = new PerWorldConfig()) : INSTANCE;
    }

    private PerWorldConfig() {
        folder = new File(EdenClient.getDataFolder(), "PerWorldCfg");
        StartGameSessionCallback.EVENT.register(this::onJoin);
        LeaveGameSessionCallback.EVENT.register(this::onLeave);
    }

    private ActionResult onLeave(ClientPlayerEntity player) {
        System.out.println("leave world: " + worldName);
        saveConfig();
        return ActionResult.PASS;
    }

    private ActionResult onJoin(ClientPlayerEntity player) {
        worldName = StringUtils.getWorldOrServerName();
        System.out.println("join world: " + worldName);
        loadConfig();
        return ActionResult.PASS;
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

    public NbtCompound getTag() {
        return tag;
    }
}
