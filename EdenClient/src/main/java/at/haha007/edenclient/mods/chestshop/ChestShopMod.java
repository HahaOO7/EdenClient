package at.haha007.edenclient.mods.chestshop;

import at.haha007.edenclient.EdenClient;
import at.haha007.edenclient.annotations.Mod;
import at.haha007.edenclient.callbacks.AddChatMessageCallback;
import at.haha007.edenclient.callbacks.PlayerTickCallback;
import at.haha007.edenclient.mods.GetTo;
import at.haha007.edenclient.mods.chestshop.pathing.ChestShopModPathing;
import at.haha007.edenclient.mods.datafetcher.ChestShopItemNames;
import at.haha007.edenclient.mods.datafetcher.DataFetcher;
import at.haha007.edenclient.mods.pathfinder.PathFinder;
import at.haha007.edenclient.utils.ChatColor;
import at.haha007.edenclient.utils.PlayerUtils;
import at.haha007.edenclient.utils.Utils;
import at.haha007.edenclient.utils.config.ConfigSubscriber;
import at.haha007.edenclient.utils.config.PerWorldConfig;
import at.haha007.edenclient.utils.tasks.SyncTask;
import at.haha007.edenclient.utils.tasks.TaskManager;
import at.haha007.edenclient.utils.tasks.WaitForTicksTask;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.chunk.ChunkSource;
import net.minecraft.world.level.chunk.LevelChunk;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static at.haha007.edenclient.command.CommandManager.*;
import static at.haha007.edenclient.utils.PlayerUtils.sendModMessage;

@Mod(dependencies = {PathFinder.class, DataFetcher.class})
public class ChestShopMod {

    @ConfigSubscriber
    private ChestShopMap shops = new ChestShopMap();
    @ConfigSubscriber("false")
    private boolean searchEnabled = true;

    private final PathFinder pathFinderMod;

    private final DataFetcher dataFetcher;

    private ChestShopModPathing runningChestShopModPathFinding = null;

    private int[] chunk = {0, 0};

    public ChestShopMod() {
        registerCommand("echestshop");
        registerCommand("ecs");
        PlayerTickCallback.EVENT.register(this::tick);
        AddChatMessageCallback.EVENT.register(this::onChat);
        pathFinderMod = EdenClient.getMod(PathFinder.class);
        dataFetcher = EdenClient.getMod(DataFetcher.class);
        PerWorldConfig.get().register(this, "chestShop");
        PerWorldConfig.get().register(new ChestShopLoader(), ChestShopMap.class);
        PerWorldConfig.get().register(new ChestShopEntryLoader(), ChestShopEntry.class);
    }

    private void onChat(AddChatMessageCallback.ChatAddEvent chatAddEvent) {
        if (runningChestShopModPathFinding != null) {
            runningChestShopModPathFinding.onChat(chatAddEvent);
        }
    }

    private void tick(LocalPlayer player) {
        int[] chunk = {player.chunkPosition().x, player.chunkPosition().z};
        if (Arrays.equals(this.chunk, chunk)) return;
        if (!searchEnabled) return;
        this.chunk = chunk;
        checkForShops(player);
    }

    private void checkForShops(ChunkSource cm, ChunkPos chunk) {
        if (!cm.hasChunk(chunk.x, chunk.z)) return;
        LevelChunk c = cm.getChunk(chunk.x, chunk.z, false);
        if (c == null) return;
        ChestShopSet shopsSet = shops.get(chunk);
        if (shopsSet == null) {
            shopsSet = new ChestShopSet();
        }

        List<ChestShopEntry> entries = c.getBlockEntities().values().stream()
                .filter(SignBlockEntity.class::isInstance)
                .map(t -> (SignBlockEntity) t)
                .map(ChestShopEntry::new)
                .filter(ChestShopEntry::isShop)
                .toList();

        shopsSet.addAll(entries);
        Set<Vec3i> allPos = entries.stream().map(ChestShopEntry::getPos).collect(Collectors.toSet());
        shopsSet.removeIf(entry -> !allPos.contains(entry.getPos()));
        shops.put(chunk, shopsSet);
    }

    private void checkForShops(LocalPlayer player, int radius) {
        ChunkSource cm = player.clientLevel.getChunkSource();
        ChunkPos.rangeClosed(player.chunkPosition(), radius).forEach(cp -> checkForShops(cm, cp));
    }

    private void checkForShops(LocalPlayer player) {
        checkForShops(player, 5);
    }

    private void registerCommand(String name) {
        LiteralArgumentBuilder<ClientSuggestionProvider> node = literal(name);

        node.then(literal("clear").executes(c -> {
            shops.clear();
            sendModMessage("Cleared ChestShop entries.");
            return 1;
        }));

        node.then(literal("visitshops").executes(c -> {
            var pwarpShops = EdenClient.getMod(DataFetcher.class).getPlayerWarps().getShops();
            TaskManager tm = new TaskManager();
            sendModMessage(ChatColor.GOLD + "Teleporting to all player warps, this will take about " +
                    ChatColor.AQUA + (pwarpShops.size() * 6) +
                    ChatColor.GOLD + " seconds.");
            int count = pwarpShops.size();
            AtomicInteger i = new AtomicInteger(1);
            for (String shop : pwarpShops.keySet()) {
                tm.then(new SyncTask(() -> PlayerUtils.messageC2S("/pwarp " + shop, false)));
                tm.then(new WaitForTicksTask(120)); //wait for chunks to load
                tm.then(new SyncTask(() -> sendModMessage(ChatColor.GOLD + "Shop " +
                        ChatColor.AQUA + i.getAndIncrement() +
                        ChatColor.GOLD + "/" +
                        ChatColor.AQUA + count +
                        ChatColor.GOLD + " | " +
                        ChatColor.AQUA + ((count - i.get() + 1) * 5) +
                        ChatColor.GOLD + " seconds left")));
                tm.then(new SyncTask(() -> checkForShops(PlayerUtils.getPlayer(), 8)));
            }
            tm.start();
            return 1;
        }));

        node.then(literal("visitbaritone").executes(c -> {
            if (runningChestShopModPathFinding != null) {
                sendModMessage(ChatColor.GOLD + "Baritone already running!");
                return 1;
            }
            if (!searchEnabled) { // otherwise makes no sense to run this cmd
                searchEnabled = true;
            }
            Map<String, Vec3i> shops = EdenClient.getMod(DataFetcher.class).getPlayerWarps().getShops();
            TaskManager tm = new TaskManager();
            sendModMessage(ChatColor.GOLD + "Teleporting to all player warps and using the "
                    + ChatColor.AQUA + "Pathfinder "
                    + ChatColor.GOLD + "to click all shops!");
            int count = shops.size();
            AtomicInteger i = new AtomicInteger(1);

            // make sure that the pathfinder is enabled. If the user disables it mid run it will be disabled!
            pathFinderMod.clear();
            pathFinderMod.enable();

            for (String shop : shops.keySet()) {
                if (!pathFinderMod.isEnabled()) {
                    tm.cancel();
                }
                tm.then(new SyncTask(() -> PlayerUtils.messageC2S("/pwarp " + shop, false)));
                tm.then(new WaitForTicksTask(40)); // wait 2 seconds for chunks to load
                tm.then(new SyncTask(() -> sendModMessage(ChatColor.GOLD + "Using baritone to click all shops.")));
                tm.then(new SyncTask(() -> this.runningChestShopModPathFinding = new ChestShopModPathing(tm, this.shops, 8, pathFinderMod, dataFetcher)));
                tm.then(new SyncTask(() -> runningChestShopModPathFinding.run()));
                tm.then(() -> {
                    while (!Thread.interrupted()) {
                        Utils.sleep(200);
                        if (!pathFinderMod.isRunning()) {
                            return;
                        }
                    }
                });
                tm.then(new SyncTask(() -> sendModMessage(ChatColor.GOLD + "Finished clicking all shops.")));
                tm.then(new SyncTask(() -> sendModMessage(ChatColor.GOLD + "Shop " +
                        ChatColor.AQUA + i.getAndIncrement() +
                        ChatColor.GOLD + "/" +
                        ChatColor.AQUA + count +
                        ChatColor.GOLD + " finished.")));
                tm.then(new SyncTask(() -> checkForShops(PlayerUtils.getPlayer(), 8)));
            }
            tm.then(new WaitForTicksTask(40));
            tm.then(() -> this.runningChestShopModPathFinding = null);
            tm.start();
            return 1;
        }));

        node.then(literal("visitbaritonesingle").then(argument("pwarp", StringArgumentType.word()).executes(c -> {
            String pwarpName = c.getArgument("pwarp", String.class);

            // make sure that the pathfinder is enabled. If the user disables it mid run it will be disabled!
            pathFinderMod.clear();
            pathFinderMod.enable();

            TaskManager tm = new TaskManager();
            tm.then(new SyncTask(() -> PlayerUtils.messageC2S("/pwarp " + pwarpName, false)));
            tm.then(() -> sendModMessage(ChatColor.GOLD + "Waiting for 2 seconds, then pathing."));
            tm.then(new WaitForTicksTask(40));
            tm.then(new SyncTask(() -> this.runningChestShopModPathFinding = new ChestShopModPathing(tm, this.shops, 8, pathFinderMod, dataFetcher)));
            tm.then(new SyncTask(() -> runningChestShopModPathFinding.run()));
            tm.then(() -> {
                while (!Thread.interrupted()) {
                    Utils.sleep(200);
                    if (!pathFinderMod.isRunning()) {
                        return;
                    }
                }
            });
            tm.then(new WaitForTicksTask(40));
            tm.then(() -> this.runningChestShopModPathFinding = null);
            tm.then(() -> sendModMessage(ChatColor.GOLD + "Finished with shop."));
            tm.start();

            return 1;
        })));


        node.then(literal("list").executes(c -> {
            int sum = shops.values().stream().mapToInt(Set::size).sum();
            if (sum < 20)
                shops.values().forEach(sl -> sl.stream().map(cs -> cs.getItem() + " B" + cs.getBuyPricePerItem() + ":" + cs.getSellPricePerItem() + "S").forEach(PlayerUtils::sendModMessage));
            sendModMessage(String.format("There are %s ChestShops stored.", sum));
            return 1;
        }));

        node.then(literal("toggle").executes(c -> {
            searchEnabled = !searchEnabled;
            sendModMessage("ChestShop search " + (searchEnabled ? "enabled" : "disabled"));
            return 1;
        }));

        node.then(literal("sell").then(argument("item", StringArgumentType.greedyString()).suggests(this::suggestSell).executes(c -> {
            sendModMessage("Sell: ");
            String item = EdenClient.getMod(DataFetcher.class).getChestShopItemNames().getShortName(c.getArgument("item", String.class));
            List<ChestShopEntry> matching = new ArrayList<>();

            shops.values().forEach(m -> m.stream().filter(ChestShopEntry::canSell).
                    filter(e -> e.getItem().equals(item)).forEach(matching::add));
            matching.stream().sorted(Comparator.comparingDouble(ChestShopEntry::getSellPricePerItem).reversed())
                    .limit(10)
                    .map(cs -> {
                        Optional<Map.Entry<String, Vec3i>> opw = getNearestPlayerWarp(cs.getPos());
                        Style style = Style.EMPTY.withColor(ChatFormatting.GOLD);

                        Vec3i pos = cs.getPos();
                        String cmd = "";
                        Component hoverText = Component.literal(opw.isPresent() ? opw.get().getKey() : "click me!").withStyle(ChatFormatting.GOLD);
                        style = style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hoverText));
                        style = style.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, cmd));
                        return Component.literal(cs.formattedString(false)).setStyle(style);
                    }).forEach(PlayerUtils::sendModMessage);
            return 1;
        })));

        node.then(literal("buy").then(argument("item", StringArgumentType.greedyString()).suggests(this::suggestBuy).executes(c -> {
            String item = EdenClient.getMod(DataFetcher.class).getChestShopItemNames().getShortName(c.getArgument("item", String.class));
            sendModMessage("Buy: ");
            List<ChestShopEntry> matching = new ArrayList<>();
            shops.values().forEach(m -> m.stream().filter(ChestShopEntry::canBuy).
                    filter(e -> e.getItem().equals(item)).forEach(matching::add));
            matching.stream().sorted(Comparator.comparingDouble(ChestShopEntry::getBuyPricePerItem))
                    .limit(10)
                    .map(cs -> {
                        Optional<Map.Entry<String, Vec3i>> opw = getNearestPlayerWarp(cs.getPos());
                        Style style = Style.EMPTY.withColor(ChatFormatting.GOLD);
                        Vec3i pos = cs.getPos();
                        //String cmd = EdenClient.getMod(GetTo.class).getToCommand(new BlockPos(pos));
                        Component hoverText = Component.literal(opw.isPresent() ? opw.get().getKey() : "click me!").withStyle(ChatFormatting.GOLD);
                        style = style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hoverText));
                        //style = style.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, cmd));
                        return Component.literal(cs.formattedString(true)).setStyle(style);
                    })
                    .forEach(PlayerUtils::sendModMessage);
            return 1;
        })));

        node.then(literal("exploitable").executes(c -> {
            List<String> exploitableItems = getExploitableShopsText();

            File folder = new File(EdenClient.getDataFolder(), "ChestShop_Exploitable");
            if (!folder.exists() && (!folder.mkdirs())) {
                Utils.getLogger().error("Failed to create ChestShop folder!");

            }
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy_MM_dd_hh_mm_ss");
            File file = new File(folder, formatter.format(new Date()) + ".txt");

            try {
                if (!file.exists() && (!file.createNewFile())) return -1;
            } catch (IOException e) {
                Utils.getLogger().error("Error while creating file: " + file.getAbsolutePath(), e);
            }

            try (FileWriter writer = new FileWriter(file); BufferedWriter bw = new BufferedWriter(writer)) {
                for (String foundDisparity : exploitableItems) {
                    bw.write(foundDisparity);
                    bw.newLine();
                }
                sendModMessage(Component.literal("Wrote file without errors. Saved at ").withStyle(ChatFormatting.GOLD).
                        append(Component.literal(file.getAbsolutePath()).setStyle(Style.EMPTY.withColor(ChatFormatting.GOLD)
                                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Click to copy")))
                                .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, file.getAbsolutePath())))));
            } catch (IOException e) {
                sendModMessage("Error while writing file. See console for more info.");
                Utils.getLogger().error("Couldn't write shop contents.", e);
            }
            return 1;
        }));

        node.then(literal("writeshopstofile").executes(c -> {
            Map<String, List<ChestShopEntry>> buyEntries = getBuyShops();
            Map<String, List<ChestShopEntry>> sellEntries = getSellShops();

            List<String> lines = new ArrayList<>();
            List<String> keys = new ArrayList<>();
            keys.addAll(buyEntries.keySet());
            keys.addAll(sellEntries.keySet());
            keys = keys.stream().sorted(Comparator.comparing(s -> s)).distinct().collect(Collectors.toList());
            ChestShopItemNames itemNameMap = EdenClient.getMod(DataFetcher.class).getChestShopItemNames();
            for (String key : keys) {
                List<ChestShopEntry> currentBuyEntries = buyEntries.get(key);
                if (currentBuyEntries != null)
                    currentBuyEntries = currentBuyEntries.stream().sorted(Comparator.comparingDouble(ChestShopEntry::getBuyPricePerItem)).collect(Collectors.toList());
                else
                    currentBuyEntries = new ArrayList<>();

                List<ChestShopEntry> currentSellEntries = sellEntries.get(key);
                if (currentSellEntries != null)
                    currentSellEntries = currentSellEntries.stream().sorted(Comparator.comparingDouble(ChestShopEntry::getSellPricePerItem).reversed()).collect(Collectors.toList());
                else
                    currentSellEntries = new ArrayList<>();

                String originalName = itemNameMap.getLongName(key);
                if (originalName == null) originalName = key;

                lines.add(originalName + ":");
                if (!currentBuyEntries.isEmpty()) {
                    lines.add("Buy:");
                    currentBuyEntries.forEach(e -> lines.add(String.format("%-15s [%6d, %3d, %6d] for %.2f$/item", e.getOwner(), e.getPos().getX(), e.getPos().getY(), e.getPos().getZ(), e.getBuyPricePerItem())));
                }
                if (!currentSellEntries.isEmpty()) {
                    lines.add("Sell:");
                    currentSellEntries.forEach(e -> lines.add(String.format("%-15s [%6d, %3d, %6d] for %.2f$/item", e.getOwner(), e.getPos().getX(), e.getPos().getY(), e.getPos().getZ(), e.getSellPricePerItem())));
                }
                lines.add("");
            }

            File folder = new File(EdenClient.getDataFolder(), "ChestShopModEntries");
            if (!folder.exists() && (!folder.mkdirs())) {
                Utils.getLogger().error("Failed to create ChestShop folder!");

            }
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy_MM_dd_hh_mm_ss");
            Date date = new Date();
            File file = new File(folder, formatter.format(date) + ".txt");

            try {
                if (!file.exists() && (!file.createNewFile())) return -1;
            } catch (IOException e) {
                Utils.getLogger().error("Error while creating file: " + file.getAbsolutePath(), e);
            }

            try (FileWriter writer = new FileWriter(file); BufferedWriter bw = new BufferedWriter(writer)) {
                for (String line : lines) {
                    if (line == null) {
                        bw.write("null:");
                        continue;
                    }
                    bw.write(line);
                    bw.newLine();
                }
                sendModMessage(Component.literal("Wrote file without errors. Saved at ").withStyle(ChatFormatting.GOLD).
                        append(Component.literal(file.getAbsolutePath()).setStyle(Style.EMPTY.withColor(ChatFormatting.GOLD)
                                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Click to copy path")))
                                .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, file.getAbsolutePath())))));
            } catch (IOException e) {
                sendModMessage("Error while writing file. See console for more info.");
                Utils.getLogger().error("Couldn't write shop contents.", e);
            }
            return 1;
        }));

        node.executes(c -> {
            sendModMessage("/chestshop sell itemtype");
            sendModMessage("/chestshop buy itemtype");
            sendModMessage("/chestshop toggle");
            sendModMessage("/chestshop clear");
            sendModMessage("/chestshop list");
            return 1;
        });
        register(node,
                "ChestShop item find/sell/buy helper.",
                "Automatically stores all ChestShops in all chunks you load. You can search specific items to get their buy/sell options. Other features include automatic searching for shops which sell items cheaper than other shops buy them, writing all shops to a file and automatically updating all shops via their playerwarps.");
    }

    private record Disparity(ChestShopEntry buy, ChestShopEntry sell, String nameOfItem, double disparity) {

    }

    private List<String> getExploitableShopsText() {
        Map<String, List<ChestShopEntry>> buyEntries = getBuyShops();
        Map<String, List<ChestShopEntry>> sellEntries = getSellShops();
        List<String> exploitableShopsText = new ArrayList<>();
        ChestShopItemNames itemNameMap = EdenClient.getMod(DataFetcher.class).getChestShopItemNames();

        List<Disparity> disparities = new ArrayList<>();

        for (Map.Entry<String, List<ChestShopEntry>> entry : buyEntries.entrySet().stream().sorted(Map.Entry.comparingByKey()).toList()) {

            if (!sellEntries.containsKey(entry.getKey())) {
                continue;
            }

            List<ChestShopEntry> currentSellEntries = sellEntries.get(entry.getKey()).stream().sorted(Comparator.comparingDouble(ChestShopEntry::getSellPricePerItem).reversed()).toList();
            List<ChestShopEntry> currentBuyEntries = entry.getValue().stream().sorted(Comparator.comparingDouble(ChestShopEntry::getBuyPricePerItem)).toList();

            ChestShopEntry currentSellEntry = currentSellEntries.get(0);
            ChestShopEntry currentBuyEntry = currentBuyEntries.get(0);
            int i = 0;

            if (currentSellEntry.getSellPricePerItem() <= currentBuyEntry.getBuyPricePerItem()) {
                continue;
            }

            String nameOfItem = itemNameMap.getLongName(entry.getKey());

            while (currentSellEntry.getSellPricePerItem() > currentBuyEntry.getBuyPricePerItem()) {
                disparities.add(new Disparity(currentBuyEntry, currentSellEntry, nameOfItem, currentSellEntry.getSellPricePerItem() - currentBuyEntry.getBuyPricePerItem()));
                i++;
                if (i < currentSellEntries.size())
                    currentSellEntry = currentSellEntries.get(i);
                else break;
            }
        }

        List<Map.Entry<String, List<Disparity>>> sortedDisparities = disparities.stream().filter(disp -> disp.nameOfItem != null).collect(Collectors.groupingBy(disp -> disp.nameOfItem)).entrySet().stream()
                .sorted(Comparator.comparingDouble(entry -> entry.getValue().stream().mapToDouble(shop -> shop.disparity).max().getAsDouble())).collect(Collectors.toList());

        // biggest disparity on top
        Collections.reverse(sortedDisparities);

        for (Map.Entry<String, List<Disparity>> entry : sortedDisparities) {
            List<Disparity> sortedEntries = entry.getValue().stream().sorted(Comparator.comparingDouble(e -> -e.disparity)).toList();
            exploitableShopsText.add(entry.getKey() + ":");
            for (Disparity disparity : sortedEntries) {
                ChestShopEntry currentBuyEntry = disparity.buy;
                ChestShopEntry currentSellEntry = disparity.sell;
                exploitableShopsText.add(String.format("Buy %s at %s [%d, %d, %d] for %.2f$/item and sell at %s [%d, %d, %d] for %.2f$/item",
                        entry.getKey(), currentBuyEntry.getOwner(), currentBuyEntry.getPos().getX(), currentBuyEntry.getPos().getY(), currentBuyEntry.getPos().getZ(), currentBuyEntry.getBuyPricePerItem(),
                        currentSellEntry.getOwner(), currentSellEntry.getPos().getX(), currentSellEntry.getPos().getY(), currentSellEntry.getPos().getZ(), currentSellEntry.getSellPricePerItem()));
            }
            exploitableShopsText.add("");
        }

        return exploitableShopsText;
    }

    private Map<String, List<ChestShopEntry>> getSellShops() {
        Map<String, List<ChestShopEntry>> sellEntries = new HashMap<>();
        shops.values().forEach(m -> m.stream().filter(ChestShopEntry::canSell).forEach(entry -> {
            List<ChestShopEntry> list;
            if (sellEntries.containsKey(entry.getItem())) {
                list = sellEntries.get(entry.getItem());
            } else {
                list = new ArrayList<>();
            }
            list.add(entry);
            sellEntries.put(entry.getItem(), list);
        }));
        return sellEntries;
    }

    private Map<String, List<ChestShopEntry>> getBuyShops() {
        Map<String, List<ChestShopEntry>> buyEntries = new HashMap<>();
        shops.values().forEach(m -> m.stream().filter(ChestShopEntry::canBuy).forEach(entry -> {
            List<ChestShopEntry> list;
            if (buyEntries.containsKey(entry.getItem())) {
                list = buyEntries.get(entry.getItem());
            } else {
                list = new ArrayList<>();
            }
            list.add(entry);
            buyEntries.put(entry.getItem(), list);
        }));
        return buyEntries;
    }

    private Optional<Map.Entry<String, Vec3i>> getNearestPlayerWarp(Vec3i pos) {
        return EdenClient.getMod(DataFetcher.class).getPlayerWarps().getAll().entrySet().stream().min(Comparator.comparingDouble(e -> e.getValue().distSqr(pos)));
    }

    private CompletableFuture<Suggestions> suggestSell
            (CommandContext<ClientSuggestionProvider> context, SuggestionsBuilder suggestionsBuilder) {
        ChestShopItemNames itemNameMap = EdenClient.getMod(DataFetcher.class).getChestShopItemNames();
        shops.values().forEach(s -> s.stream().filter(ChestShopEntry::canSell).map(entry -> itemNameMap.getLongName(entry.getItem())).filter(Objects::nonNull).forEach(suggestionsBuilder::suggest));
        return suggestionsBuilder.buildFuture();
    }

    private CompletableFuture<Suggestions> suggestBuy
            (CommandContext<ClientSuggestionProvider> context, SuggestionsBuilder suggestionsBuilder) {
        ChestShopItemNames itemNameMap = EdenClient.getMod(DataFetcher.class).getChestShopItemNames();
        shops.values().forEach(s -> s.stream().filter(ChestShopEntry::canBuy)
                .map(entry -> itemNameMap.getLongName(entry.getItem()))
                .filter(Objects::nonNull).forEach(suggestionsBuilder::suggest));
        return suggestionsBuilder.buildFuture();
    }
}