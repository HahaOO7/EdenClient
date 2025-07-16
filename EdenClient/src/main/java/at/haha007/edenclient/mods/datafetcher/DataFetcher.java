package at.haha007.edenclient.mods.datafetcher;

import at.haha007.edenclient.annotations.Mod;
import lombok.Getter;

import static at.haha007.edenclient.command.CommandManager.literal;
import static at.haha007.edenclient.command.CommandManager.register;

@Getter
@Mod
public class DataFetcher {
    private final PlayerWarps playerWarps = new PlayerWarps();
    private final ChestShopItemNames chestShopItemNames = new ChestShopItemNames();
    private final ContainerInfo containerInfo = new ContainerInfo();

    public DataFetcher() {
        register(literal("edatafetcher")
                        .then(playerWarps.registerCommand())
                        .then(containerInfo.registerCommand())
                        .then(chestShopItemNames.registerCommand()),
                "Enables fetching of playerwarp-data and abbreviated chestshop-itemnames.");
    }

}
