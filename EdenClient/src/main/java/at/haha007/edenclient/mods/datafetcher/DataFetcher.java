package at.haha007.edenclient.mods.datafetcher;

import at.haha007.edenclient.annotations.Mod;

import static at.haha007.edenclient.command.CommandManager.literal;
import static at.haha007.edenclient.command.CommandManager.register;

@Mod
public class DataFetcher {
    private final PlayerWarps playerWarps = new PlayerWarps();
    private final ChestShopItemNames chestShopItemNames = new ChestShopItemNames();
    private final ContainerInfo containerInfo = new ContainerInfo();

    public DataFetcher() {
        register(literal("edatafetcher").then(playerWarps.registerCommand()).then(chestShopItemNames.registerCommand()),
                "Enables fetching of playerwarp-data and abbreviated chestshop-itemnames.");
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
