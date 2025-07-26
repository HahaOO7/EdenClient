package at.haha007.edenclient.mods;

import at.haha007.edenclient.EdenClient;
import at.haha007.edenclient.annotations.Mod;
import at.haha007.edenclient.command.CommandManager;
import at.haha007.edenclient.mods.datafetcher.ContainerInfo.ChunkChestMap;
import at.haha007.edenclient.mods.datafetcher.DataFetcher;
import at.haha007.edenclient.utils.PlayerUtils;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.phys.Vec3;

import java.util.*;
import java.util.stream.Stream;

@Mod(dependencies = {DataFetcher.class, GetTo.class})
public class SearchItem {
    private SearchItem() {
        registerCommand();
    }

    private void registerCommand() {
        var cmd = CommandManager.literal("esearchitem");
        for (Item item : BuiltInRegistries.ITEM) {
            cmd.then(CommandManager.literal(BuiltInRegistries.ITEM.getKey(item).getPath()).executes(c -> {
                Vec3 playerPosf = PlayerUtils.getPlayer().getPosition(0f);
                Vec3i playerPos = new Vec3i((int) playerPosf.x(), (int) playerPosf.y(), (int) playerPosf.z());
                Optional<Vec3i> nearest = findContainersWithItem(item)
                        .min(Comparator.comparingDouble(pos -> pos.distSqr(playerPos)));
                nearest.ifPresentOrElse(pos -> {
                    String getToCommand = EdenClient.getMod(GetTo.class).getCommandTo(pos);
                    PlayerUtils.messageC2S(getToCommand);
                    PlayerUtils.sendModMessage("Nearest container with " + item.getName().getString() + " found");
                }, () -> PlayerUtils.sendModMessage("No containers with " + item.getName().getString() + " found"));
                return 1;
            }));
        }
        CommandManager.register(cmd);
    }

    private Stream<Vec3i> findContainersWithItem(Item item) {
        ChunkChestMap chunkChestMap = EdenClient.getMod(DataFetcher.class).getContainerInfo().chunkMap();
        if (chunkChestMap == null) return Stream.empty();
        return chunkChestMap.values()
                .stream()
                .map(HashMap::entrySet)
                .flatMap(Set::stream)
                .filter(e -> e.getValue().items().contains(item))
                .map(Map.Entry::getKey);
    }

}
