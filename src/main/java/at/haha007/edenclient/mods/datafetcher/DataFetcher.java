package at.haha007.edenclient.mods.datafetcher;

import static at.haha007.edenclient.command.CommandManager.literal;
import static at.haha007.edenclient.command.CommandManager.register;

public class DataFetcher {
    private final PlayerWarps playerWarps = new PlayerWarps();
    private final ChestShopItemNames chestShopItemNames = new ChestShopItemNames();
    private final ContainerInfo containerInfo = new ContainerInfo();

    public DataFetcher() {
        register(literal("datafetcher").then(playerWarps.registerCommand()));
        register(literal("datafetcher").then(chestShopItemNames.registerCommand()));
    }

    public ContainerInfo getContainerInfo() {
        return containerInfo;
    }

    public PlayerWarps getPlayerWarps() {
        return playerWarps;
    }

    public ChestShopItemNames getChestShopItemNames() {
        return chestShopItemNames;
    }
}
