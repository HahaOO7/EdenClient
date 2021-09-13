package at.haha007.edenclient.mods.datafetcher;

import static at.haha007.edenclient.command.CommandManager.literal;
import static at.haha007.edenclient.command.CommandManager.register;

public class DataFetcher {
    private final PlayerWarps playerWarps = new PlayerWarps();
    private final ChestShopItemNames chestShopItemNames = new ChestShopItemNames();

    public DataFetcher() {
        register(literal("edatafetcher").then(playerWarps.registerCommand()));
        register(literal("edatafetcher").then(chestShopItemNames.registerCommand()));
    }

    public PlayerWarps getPlayerWarps() {
        return playerWarps;
    }

    public ChestShopItemNames getChestShopItemNames() {
        return chestShopItemNames;
    }
}
