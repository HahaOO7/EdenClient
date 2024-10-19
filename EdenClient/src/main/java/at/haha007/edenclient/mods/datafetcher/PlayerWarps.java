package at.haha007.edenclient.mods.datafetcher;

import at.haha007.edenclient.command.CommandManager;
import at.haha007.edenclient.utils.PlayerUtils;
import at.haha007.edenclient.utils.PluginSignature;
import at.haha007.edenclient.utils.config.ConfigSubscriber;
import at.haha007.edenclient.utils.config.PerWorldConfig;
import at.haha007.edenclient.utils.config.loaders.ConfigLoader;
import at.haha007.edenclient.utils.config.loaders.StringListLoader;
import at.haha007.edenclient.utils.config.wrappers.StringList;
import at.haha007.edenclient.utils.tasks.*;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.logging.LogUtils;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static at.haha007.edenclient.utils.PlayerUtils.removeColorCodes;
import static java.lang.Integer.parseInt;

@Getter
public class PlayerWarps {

    @ConfigSubscriber
    private PlayerWarpList warps = new PlayerWarpList();

    PlayerWarps() {
        PerWorldConfig.get().register(this, "dataFetcher.playerWarps");
        PerWorldConfig.get().register(new PlayerWarpLoader(), PlayerWarp.class);
        PerWorldConfig.get().register(new PlayerWarpListLoader(), PlayerWarpList.class);
    }

    LiteralArgumentBuilder<ClientSuggestionProvider> registerCommand() {
        LiteralArgumentBuilder<ClientSuggestionProvider> cmd = CommandManager.literal("playerwarps");
        //PWARP
        cmd.then(CommandManager.fakeLiteral("shops").executes(c -> fetchPwarpData(warps, "warps", 10)).requires(c -> PluginSignature.PWARP.isPluginPresent()));
        cmd.then(CommandManager.fakeLiteral("builds").executes(c -> fetchPwarpData(warps, "builds", 12)).requires(c -> PluginSignature.PWARP.isPluginPresent()));
        cmd.then(CommandManager.fakeLiteral("farms").executes(c -> fetchPwarpData(warps, "farms", 14)).requires(c -> PluginSignature.PWARP.isPluginPresent()));
        cmd.then(CommandManager.fakeLiteral("other").executes(c -> fetchPwarpData(warps, "other", 16)).requires(c -> PluginSignature.PWARP.isPluginPresent()));
        cmd.then(CommandManager.fakeLiteral("all").executes(c -> fetchPwarpData(warps, "all", 4)).requires(c -> PluginSignature.PWARP.isPluginPresent()));
        cmd.then(CommandManager.fakeLiteral("hidden").executes(c -> fetchPwarpData(warps, "hidden", 22)).requires(c -> PluginSignature.PWARP.isPluginPresent()));
        cmd.then(CommandManager.fakeLiteral("*").executes(c -> fetchPwarpData()).requires(c -> PluginSignature.PWARP.isPluginPresent()));

        //PLAYER_WARPS
        cmd.then(CommandManager.fakeLiteral("fetch").executes(c -> fetchPlayerWarpData()).requires(c -> PluginSignature.PLAYER_WARPS.isPluginPresent()));

        cmd.then(CommandManager.fakeLiteral("clear").executes(c -> clearData()));
        return cmd;
    }


    //PLAYER_WARPS SECTION
    private void queueNextPage(TaskManager tm, int page, Set<String> memo) {
        LogUtils.getLogger().warn("Page: {}", page);
        tm.then(new WaitForTicksTask(20));
        tm.then(new SyncTask(() -> PlayerUtils.messageC2S("/pwarp list " + page)));
        tm.then(new MaxTimeTask(new WaitForChatTask(msg -> {
            String text = removeColorCodes(msg.getString());
            LogUtils.getLogger().warn("Message: {}", text);
            if (text.matches("PW » That page does not exist![\\S\\s]*")) return true;
            if (!text.matches("PW .?» Akutelle Warps: [\\S\\s]*")) return false;
            queueNextPage(tm, page + 1, memo);
            Arrays.stream(text.split("[\n\r]"))
                    .map(s -> s.split("\\s+"))
                    .map(a -> a[a.length - 1])
                    .forEach(memo::add);
            return true;
        }), 1000));
    }

    private int fetchPlayerWarpData() {
        clearData();
        TaskManager tm = new TaskManager();
        //first get all the pwarps by executing the list command until there are no more warps
        TaskManager listWarpManager = new TaskManager();
        Set<String> warpNames = new HashSet<>();
        queueNextPage(listWarpManager, 1, warpNames);

        TaskManager infoTask = new TaskManager();
        infoTask.then(new SyncTask(() -> {
            for (String warpName : warpNames) {
                LogUtils.getLogger().info("Queued warp info: {}", warpName);
                infoTask.then(new WaitForTicksTask(20));
                infoTask.then(new SyncTask(() -> PlayerUtils.messageC2S("/pwarp info " + warpName)));
                infoTask.then(new MaxTimeTask(new WaitForChatTask(msg -> {
                    String text = removeColorCodes(msg.getString());
                    LogUtils.getLogger().info("Fetching warp info: {} {}", warpName, text);
                    if (text.matches("PW » Dieser Warp existiert nicht![\\S\\s]*")) return true;
                    if (!text.matches("PW » Warp Information for %s:[\\S\\s]*"
                            .formatted(Pattern.quote(warpName.replace('_', ' ').replaceAll("[\\[\\]{}()]", "")))))
                        return false;
                    LogUtils.getLogger().info("Received warp info: {}", text);
                    Vec3i position = Arrays.stream(text.split("[\r\n]+"))
                            .filter(s -> s.matches("PW » Location: [\\S\\s]*"))
                            .findFirst()
                            .map(a -> a.split(": ")[1])
                            .map(a -> a.split(", "))
                            .map(a -> new int[]{parseInt(a[1]), parseInt(a[2]), parseInt(a[3])})
                            .map(a -> new Vec3i(a[0], a[1], a[2]))
                            .orElse(null);
                    String category = Arrays.stream(text.split("[\r\n]+")).filter(s -> s.matches("PW » Category: [\\S\\s]*"))
                            .findFirst()
                            .map(a -> a.split(": ")[1])
                            .orElse(null);
                    if (position == null || category == null) {
                        PlayerUtils.sendModMessage("Failed to get position or category for " + warpName);
                        return true;
                    }
                    LogUtils.getLogger().info("Got warp info for {}: {}, {}", warpName, position, category);
                    PlayerWarp pw = new PlayerWarp(warpName, position, new StringList(category));
                    this.warps.add(pw);
                    return true;
                }), 5000));
            }
        }));

        tm.then(listWarpManager);
        tm.then(infoTask);

        tm.start();

        //second get the position by executing the info command for each player-warp
        return 1;
    }


    //PWARP SECTION

    private int clearData() {
        warps = new PlayerWarpList();
        PlayerUtils.sendModMessage("Cleared data!");
        return 1;
    }

    private int fetchPwarpData() {
        TaskManager tm = new TaskManager();
        Queue<Task> q = new LinkedList<>();
        q.add(new SyncTask(() -> fetchPwarpData(tm, q, warps, "warps", 10)));
        q.add(new SyncTask(() -> fetchPwarpData(tm, q, warps, "builds", 12)));
        q.add(new SyncTask(() -> fetchPwarpData(tm, q, warps, "farms", 14)));
        q.add(new SyncTask(() -> fetchPwarpData(tm, q, warps, "other", 16)));
        q.add(new SyncTask(() -> fetchPwarpData(tm, q, warps, "hidden", 22)));
        q.add(new SyncTask(() -> {
            Screen screen = Minecraft.getInstance().screen;
            if (screen == null) return;
            screen.onClose();
        }));
        fetchPwarpData(tm, q, warps, "all", 4);
        tm.start();
        return 1;
    }

    private int fetchPwarpData(PlayerWarpList map, String category, int slot) {
        TaskManager tm = new TaskManager();
        fetchPwarpData(tm, new LinkedList<>(List.of(new SyncTask(() -> {
            Screen screen = Minecraft.getInstance().screen;
            if (screen == null) return;
            screen.onClose();
        }))), map, category, slot);
        tm.start();
        return 1;
    }

    private void fetchPwarpData(TaskManager tm, Queue<Task> endTask, PlayerWarpList map, String category, int slot) {
        map.clear();
        tm.then(new SyncTask(() -> PlayerUtils.messageC2S("/pwarp")));
        tm.then(new WaitForInventoryTask(Pattern.compile(".*")));
        tm.then(new SyncTask(() -> PlayerUtils.clickSlot(slot)));
        tm.then(new WaitForInventoryTask(Pattern.compile(". PlayerWarps - Seite 1/\\d{1,2}")));
        tm.then(new SyncTask(() -> {
            Pattern pattern = Pattern.compile(". PlayerWarps - Seite 1/(?<pages>[0-9]{1,2})");
            Screen screen = Minecraft.getInstance().screen;
            if (screen == null) return;
            Matcher matcher = pattern.matcher(screen.getTitle().getString());
            if (!matcher.matches()) return;
            int pages = parseInt(matcher.group("pages"));

            tm.then(new WaitForTicksTask(5));
            tm.then(new SyncTask(() -> scanWarps(map, category)));
            for (int i = 2; i <= pages; i++) {
                tm.then(new SyncTask(() -> PlayerUtils.clickSlot(50)));
                tm.then(new WaitForInventoryTask(Pattern.compile(". PlayerWarps - Seite " + i + "/" + pages)));
                tm.then(new WaitForTicksTask(5));
                tm.then(new SyncTask(() -> scanWarps(map, category)));
            }
            tm.then(endTask.poll());
        }));
    }


    private void scanWarps(PlayerWarpList map, String category) {
        Screen sc = Minecraft.getInstance().screen;
        if (sc == null) return;
        if (!(sc instanceof ContainerScreen containerScreen)) return;
        Container inventory = containerScreen.getMenu().getContainer();
        for (int i = 0; i < 45; i++) {
            ItemStack item = inventory.getItem(i);
            if (item.isEmpty()) continue;
            String name = item.getHoverName().getString();
            item.getTooltipLines(null, PlayerUtils.getPlayer(), TooltipFlag.NORMAL).stream().map(Component::getString).filter(s -> s.startsWith("Ort: world, ")).findAny().ifPresent(s -> map.add(new PlayerWarp(name, getPwarpPos(s), new StringList(category))));
        }
    }

    @NotNull
    private Vec3i getPwarpPos(String s) {
        s = s.substring(12);
        Pattern pattern = Pattern.compile("(?<x>-?\\d+), (?<y>-?\\d+), (?<z>-?\\d+)");
        Matcher matcher = pattern.matcher(s);
        if (!matcher.matches()) return Vec3i.ZERO;
        return new Vec3i(parseInt(matcher.group("x")), parseInt(matcher.group("y")), parseInt(matcher.group("z")));
    }

    public static class PlayerWarpList extends ArrayList<PlayerWarp> {
    }

    public static class PlayerWarpListLoader implements ConfigLoader<ListTag, PlayerWarpList> {
        private static final PlayerWarpLoader playerWarpLoader = new PlayerWarpLoader();

        @Override
        public @NotNull ListTag save(@NotNull PlayerWarpList value) {
            ListTag nbt = new ListTag();
            for (PlayerWarp warp : value) {
                nbt.add(playerWarpLoader.save(warp));
            }
            return nbt;
        }

        @Override
        public @NotNull PlayerWarpList load(@NotNull ListTag nbtList) {
            PlayerWarpList list = new PlayerWarpList();
            nbtList.forEach(nbt -> list.add(playerWarpLoader.load((CompoundTag) nbt)));
            return list;
        }

        @Override
        public @NotNull ListTag parse(@NotNull String s) {
            return new ListTag();
        }
    }

    public static record PlayerWarp(String name, Vec3i pos, StringList categories) {
    }

    public static class PlayerWarpLoader implements ConfigLoader<CompoundTag, PlayerWarp> {
        private static final StringListLoader stringListLoader = new StringListLoader();
        public static final String CATEGORIES = "categories";

        @Override
        public @NotNull CompoundTag save(@NotNull PlayerWarp value) {
            CompoundTag nbt = new CompoundTag();
            nbt.putString("name", value.name);
            nbt.putInt("x", value.pos.getX());
            nbt.putInt("y", value.pos.getY());
            nbt.putInt("z", value.pos.getZ());
            nbt.put(CATEGORIES, stringListLoader.save(value.categories));
            return nbt;
        }

        @Override
        public @NotNull PlayerWarp load(@NotNull CompoundTag nbtElement) {
            String name = nbtElement.getString("name");
            int x = nbtElement.getInt("x");
            int y = nbtElement.getInt("y");
            int z = nbtElement.getInt("z");
            StringList categories = stringListLoader.load(nbtElement.getList(CATEGORIES, Tag.TAG_STRING));
            return new PlayerWarp(name, new Vec3i(x, y, z), categories);
        }

        @Override
        public @NotNull CompoundTag parse(@NotNull String s) {
            CompoundTag nbt = new CompoundTag();
            nbt.putString("name", "default");
            nbt.putInt("x", 0);
            nbt.putInt("y", 0);
            nbt.putInt("z", 0);
            nbt.put(CATEGORIES, new ListTag());
            return nbt;
        }
    }
}
