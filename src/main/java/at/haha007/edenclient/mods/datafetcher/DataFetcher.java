package at.haha007.edenclient.mods.datafetcher;

import at.haha007.edenclient.callbacks.ConfigLoadCallback;
import at.haha007.edenclient.callbacks.ConfigSaveCallback;
import net.minecraft.nbt.NbtCompound;

import static at.haha007.edenclient.command.CommandManager.literal;
import static at.haha007.edenclient.command.CommandManager.register;

public class DataFetcher {
    private final PlayerWarps playerWarps = new PlayerWarps();
    private final ChestShopItemNames chestShopItemNames = new ChestShopItemNames();

    public DataFetcher() {
        register(literal("datafetcher").then(playerWarps.registerCommand()));
        register(literal("datafetcher").then(chestShopItemNames.registerCommand()));
        ConfigLoadCallback.EVENT.register(this::load);
        ConfigSaveCallback.EVENT.register(this::save);
    }

    private void save(NbtCompound nbtCompound) {
        NbtCompound tag = new NbtCompound();
        tag.put("playerWarps", playerWarps.save());
        tag.put("chestShopItemNames", chestShopItemNames.save());
        nbtCompound.put("dataFetcher", tag);
    }

    private void load(NbtCompound nbtCompound) {
        NbtCompound tag = nbtCompound.getCompound("dataFetcher");
        playerWarps.load(tag.getCompound("playerWarps"));
        chestShopItemNames.load(tag.getCompound("chestShopItemNames"));
    }

    public PlayerWarps getPlayerWarps() {
        return playerWarps;
    }

    public ChestShopItemNames getChestShopItemNames() {
        return chestShopItemNames;
    }
}
