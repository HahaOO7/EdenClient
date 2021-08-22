package at.haha007.edenclient.mods.datafetcher;

import at.haha007.edenclient.callbacks.ConfigLoadCallback;
import at.haha007.edenclient.callbacks.ConfigSaveCallback;
import net.minecraft.nbt.NbtCompound;

import static at.haha007.edenclient.command.CommandManager.literal;
import static at.haha007.edenclient.command.CommandManager.register;

public class DataFetcher {
    PlayerWarps playerWarps = new PlayerWarps();

    public DataFetcher() {
        register(literal("datafetcher").then(playerWarps.registerCommand()));
        ConfigLoadCallback.EVENT.register(this::load);
        ConfigSaveCallback.EVENT.register(this::save);
    }

    private void save(NbtCompound nbtCompound) {
        NbtCompound tag = new NbtCompound();
        tag.put("playerWarps", playerWarps.save());
        nbtCompound.put("dataFetcher", tag);
    }

    private void load(NbtCompound nbtCompound) {
        NbtCompound tag = nbtCompound.getCompound("dataFetcher");
        playerWarps.load(tag.getCompound("playerWarps"));
    }

    public PlayerWarps getPlayerWarps() {
        return playerWarps;
    }
}
