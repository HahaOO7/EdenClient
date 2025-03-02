package at.haha007.edenclient.mods.chestshop;

import at.haha007.edenclient.EdenClient;
import at.haha007.edenclient.annotations.Mod;
import at.haha007.edenclient.callbacks.PlayerTickCallback;
import at.haha007.edenclient.mods.GetTo;
import at.haha007.edenclient.mods.datafetcher.ChestShopItemNames;
import at.haha007.edenclient.mods.datafetcher.DataFetcher;
import at.haha007.edenclient.mods.datafetcher.PlayerWarps;
import at.haha007.edenclient.utils.PlayerUtils;
import at.haha007.edenclient.utils.PluginSignature;
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
import com.mojang.logging.LogUtils;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Vec3i;
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

@Mod
public class ChestShopMod {

    @ConfigSubscriber
    private ChestShopMap shops = new ChestShopMap();
    @ConfigSubscriber("false")
    private boolean searchEnabled = true;

    private int[] chunk = {0, 0};

    public ChestShopMod() {
        registerCommand("echestshop");
        registerCommand("ecs");
        PlayerTickCallback.EVENT.register(this::tick, getClass(), PluginSignature.CHESTSHOP::isPluginPresent);
        PerWorldConfig.get().register(this, "chestShop");
        PerWorldConfig.get().register(new ChestShopLoader(), ChestShopMap.class);
        PerWorldConfig.get().register(new ChestShopEntryLoader(), ChestShopEntry.class);
    }

    private void tick(LocalPlayer player) {
        int[] playerChunk = {player.chunkPosition().x, player.chunkPosition().z};
        if (Arrays.equals(this.chunk, playerChunk)) return;
        if (!searchEnabled) return;
        this.chunk = playerChunk;
        checkForShops(player);
    }

    private void checkForShops(ChunkSource cm, ChunkPos chunk) {
        if (!cm.hasChunk(chunk.x, chunk.z)) return;
        LevelChunk c = cm.getChunk(chunk.x, chunk.z, false);
        if (c == null) return;
        shops.remove(chunk);
        ChestShopSet cs = new ChestShopSet();
        c.getBlockEntities().values().stream()
                .filter(SignBlockEntity.class::isInstance)
                .map(t -> (SignBlockEntity) t)
                .map(ChestShopEntry::new)
                .filter(ChestShopEntry::isShop)
                .forEach(cs::add);
        shops.put(chunk, cs);
    }

    private void checkForShops(LocalPlayer player, int radius) {
        ChunkSource cm = player.clientLevel.getChunkSource();
        ChunkPos.rangeClosed(player.chunkPosition(), radius).forEach(cp -> checkForShops(cm, cp));
    }

    private void checkForShops(LocalPlayer player) {
        checkForShops(player, 5);
    }

    private void registerCommand(String name) {
        LiteralArgumentBuilder<FabricClientCommandSource> node = literal(name);

        node.then(literal("clear").executes(c -> {
            shops.clear();
            sendModMessage("Cleared ChestShop entries.");
            return 1;
        }));

        node.then(literal("visitshops").executes(c -> {
            List<PlayerWarps.PlayerWarp> knownShops = EdenClient.getMod(DataFetcher.class).getPlayerWarps().getWarps()
                    .stream()
                    .filter(shop -> shop.categories().stream().noneMatch(category -> category.toLowerCase().contains("shop")))
                    .toList();
            TaskManager tm = new TaskManager();
            sendModMessage(Component.text("Teleporting to all player warps, this will take about ", NamedTextColor.GOLD)
                    .append(Component.text(knownShops.size() * 6, NamedTextColor.AQUA))
                    .append(Component.text(" seconds.", NamedTextColor.GOLD)));
            int count = knownShops.size();
            AtomicInteger i = new AtomicInteger(1);
            for (PlayerWarps.PlayerWarp shop : knownShops) {
                tm.then(new SyncTask(() -> PlayerUtils.messageC2S("/pwarp " + shop.name())));
                tm.then(new WaitForTicksTask(120)); //wait for chunks to load
                tm.then(new SyncTask(() ->
                        sendModMessage(Component.text("Shop ", NamedTextColor.GOLD)
                                .append(Component.text(i.getAndIncrement(), NamedTextColor.AQUA))
                                .append(Component.text("/", NamedTextColor.GOLD))
                                .append(Component.text(count, NamedTextColor.AQUA))
                                .append(Component.text(" | ", NamedTextColor.GOLD))
                                .append(Component.text((count - i.get() + 1) * 5, NamedTextColor.AQUA))
                                .append(Component.text(" seconds left", NamedTextColor.GOLD)))
                ));
                tm.then(new SyncTask(() -> checkForShops(PlayerUtils.getPlayer(), 8)));
            }
            tm.start();
            return 1;
        }));

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
                        Optional<PlayerWarps.PlayerWarp> opw = getNearestPlayerWarp(cs.getPos());
                        Style style = Style.style(NamedTextColor.GOLD);

                        Vec3i pos = cs.getPos();
                        String cmd = EdenClient.getMod(GetTo.class).getCommandTo(pos);
                        Component hoverText = Component.text(opw.isPresent() ? opw.get().name() : "click me!", NamedTextColor.GOLD);
                        style = style.hoverEvent(HoverEvent.hoverEvent(HoverEvent.Action.SHOW_TEXT, hoverText));
                        style = style.clickEvent(ClickEvent.clickEvent(ClickEvent.Action.RUN_COMMAND, cmd));
                        return Component.text(cs.formattedString(false)).style(style);
                    }).forEach(PlayerUtils::sendModMessage);
            return 1;
        })));

        node.then(literal("sellexact").then(argument("item", StringArgumentType.greedyString()).suggests(this::suggestSell).executes(c -> {
            sendModMessage("Sell: ");
            String item = c.getArgument("item", String.class);
            List<ChestShopEntry> matching = new ArrayList<>();

            shops.values().forEach(m -> m.stream().filter(ChestShopEntry::canSell).
                    filter(e -> e.getItem().equals(item)).forEach(matching::add));
            matching.stream().sorted(Comparator.comparingDouble(ChestShopEntry::getSellPricePerItem).reversed())
                    .limit(10)
                    .map(cs -> {
                        Optional<PlayerWarps.PlayerWarp> opw = getNearestPlayerWarp(cs.getPos());
                        Style style = Style.style(NamedTextColor.GOLD);

                        Vec3i pos = cs.getPos();
                        String cmd = EdenClient.getMod(GetTo.class).getCommandTo(pos);
                        Component hoverText = Component.text(opw.isPresent() ? opw.get().name() : "click me!", NamedTextColor.GOLD);
                        style = style.hoverEvent(HoverEvent.hoverEvent(HoverEvent.Action.SHOW_TEXT, hoverText));
                        style = style.clickEvent(ClickEvent.clickEvent(ClickEvent.Action.RUN_COMMAND, cmd));
                        return Component.text(cs.formattedString(false)).style(style);
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
                        Optional<PlayerWarps.PlayerWarp> opw = getNearestPlayerWarp(cs.getPos());
                        Style style = Style.style(NamedTextColor.GOLD);
                        Vec3i pos = cs.getPos();
                        String cmd = EdenClient.getMod(GetTo.class).getCommandTo(pos);
                        Component hoverText = Component.text(opw.isPresent() ? opw.get().name() : "click me!", NamedTextColor.GOLD);
                        style = style.hoverEvent(HoverEvent.hoverEvent(HoverEvent.Action.SHOW_TEXT, hoverText));
                        style = style.clickEvent(ClickEvent.clickEvent(ClickEvent.Action.RUN_COMMAND, cmd));
                        return Component.text(cs.formattedString(true)).style(style);
                    })
                    .forEach(PlayerUtils::sendModMessage);
            return 1;
        })));

        node.then(literal("buyexact").then(argument("item", StringArgumentType.greedyString()).suggests(this::suggestBuy).executes(c -> {
            String item = c.getArgument("item", String.class);
            sendModMessage("Buy: ");
            List<ChestShopEntry> matching = new ArrayList<>();
            shops.values().forEach(m -> m.stream().filter(ChestShopEntry::canBuy).
                    filter(e -> e.getItem().equals(item)).forEach(matching::add));
            matching.stream().sorted(Comparator.comparingDouble(ChestShopEntry::getBuyPricePerItem))
                    .limit(10)
                    .map(cs -> {
                        Optional<PlayerWarps.PlayerWarp> opw = getNearestPlayerWarp(cs.getPos());
                        Style style = Style.style(NamedTextColor.GOLD);
                        Vec3i pos = cs.getPos();
                        String cmd = EdenClient.getMod(GetTo.class).getCommandTo(pos);
                        Component hoverText = Component.text(opw.isPresent() ? opw.get().name() : "click me!", NamedTextColor.GOLD);
                        style = style.hoverEvent(HoverEvent.hoverEvent(HoverEvent.Action.SHOW_TEXT, hoverText));
                        style = style.clickEvent(ClickEvent.clickEvent(ClickEvent.Action.RUN_COMMAND, cmd));
                        return Component.text(cs.formattedString(true)).style(style);
                    })
                    .forEach(PlayerUtils::sendModMessage);
            return 1;
        })));

        node.then(literal("exploitable").executes(c -> {
            List<String> exploitableItems = getExploitableShopsText();

            File folder = new File(EdenClient.getDataFolder(), "ChestShop_Exploitable");
            if (!folder.exists() && (!folder.mkdirs())) {
                LogUtils.getLogger().error("Failed to create ChestShop_Exploitable folder!");
            }
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy_MM_dd_hh_mm_ss");
            File file = new File(folder, formatter.format(new Date()) + ".txt");

            try {
                if (!file.exists() && (!file.createNewFile())) return -1;
            } catch (IOException e) {
                LogUtils.getLogger().error("Error while creating file: {}", file.getAbsolutePath(), e);
            }

            try (FileWriter writer = new FileWriter(file); BufferedWriter bw = new BufferedWriter(writer)) {
                for (String foundDisparity : exploitableItems) {
                    bw.write(foundDisparity);
                    bw.newLine();
                }
                sendModMessage(Component.text("Wrote file without errors. Saved at ", NamedTextColor.GOLD).
                        append(Component.text(file.getAbsolutePath(), NamedTextColor.GOLD)
                                .hoverEvent(HoverEvent.hoverEvent(HoverEvent.Action.SHOW_TEXT, Component.text("Click to copy")))
                                .clickEvent(ClickEvent.clickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, file.getAbsolutePath()))));
            } catch (IOException e) {
                sendModMessage("Error while writing file. See console for more info.");
                LogUtils.getLogger().error("Couldn't write shop contents.", e);
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
                    currentBuyEntries = currentBuyEntries.stream().sorted(Comparator.comparingDouble(ChestShopEntry::getBuyPricePerItem)).toList();
                else
                    currentBuyEntries = new ArrayList<>();

                List<ChestShopEntry> currentSellEntries = sellEntries.get(key);
                if (currentSellEntries != null)
                    currentSellEntries = currentSellEntries.stream().sorted(Comparator.comparingDouble(ChestShopEntry::getSellPricePerItem).reversed()).toList();
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
                LogUtils.getLogger().error("Failed to create ChestShopModEntries folder!");
            }
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy_MM_dd_hh_mm_ss");
            Date date = new Date();
            File file = new File(folder, formatter.format(date) + ".txt");

            try {
                if (!file.exists() && (!file.createNewFile())) return -1;
            } catch (IOException e) {
                LogUtils.getLogger().error("Error while creating file: {}", file.getAbsolutePath(), e);
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
                sendModMessage(Component.text("Wrote file without errors. Saved at ", NamedTextColor.GOLD).
                        append(Component.text(file.getAbsolutePath(), NamedTextColor.GOLD)
                                .hoverEvent(HoverEvent.hoverEvent(HoverEvent.Action.SHOW_TEXT, Component.text("Click to copy path")))
                                .clickEvent(ClickEvent.clickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, file.getAbsolutePath()))));
            } catch (IOException e) {
                sendModMessage("Error while writing file. See console for more info.");
                LogUtils.getLogger().error("Couldn't write shop contents.", e);
            }
            return 1;
        }));

        node.requires(c -> PluginSignature.CHESTSHOP.isPluginPresent());
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
                "Automatically stores all ChestShops in all chunks you load. You can search specific items to get their buy/sell options." +
                        " Other features include automatic searching for shops which sell items cheaper than other shops buy them," +
                        " writing all shops to a file and automatically updating all shops via their playerwarps.");
    }

    private List<String> getExploitableShopsText() {
        Map<String, List<ChestShopEntry>> buyEntries = getBuyShops();
        Map<String, List<ChestShopEntry>> sellEntries = getSellShops();
        List<String> exploitableShopsText = new ArrayList<>();
        ChestShopItemNames itemNameMap = EdenClient.getMod(DataFetcher.class).getChestShopItemNames();
        for (Map.Entry<String, List<ChestShopEntry>> entry : buyEntries.entrySet().stream().sorted(Map.Entry.comparingByKey()).toList()) {
            if (!sellEntries.containsKey(entry.getKey())) continue;
            List<ChestShopEntry> currentSellEntries = sellEntries.get(entry.getKey()).stream().sorted(Comparator.comparingDouble(ChestShopEntry::getSellPricePerItem).reversed()).toList();
            List<ChestShopEntry> currentBuyEntries = entry.getValue().stream().sorted(Comparator.comparingDouble(ChestShopEntry::getBuyPricePerItem)).toList();

            ChestShopEntry currentSellEntry = currentSellEntries.getFirst();
            ChestShopEntry currentBuyEntry = currentBuyEntries.getFirst();
            int i = 0;

            if (currentSellEntry.getSellPricePerItem() <= currentBuyEntry.getBuyPricePerItem())
                continue;

            String nameOfItem = itemNameMap.getLongName(entry.getKey());
            exploitableShopsText.add(nameOfItem + ":");

            while (currentSellEntry.getSellPricePerItem() > currentBuyEntry.getBuyPricePerItem()) {
                exploitableShopsText.add(String.format("Buy %s at %s [%d, %d, %d] for %.2f$/item and sell at %s [%d, %d, %d] for %.2f$/item",
                        nameOfItem, currentBuyEntry.getOwner(), currentBuyEntry.getPos().getX(), currentBuyEntry.getPos().getY(), currentBuyEntry.getPos().getZ(), currentBuyEntry.getBuyPricePerItem(),
                        currentSellEntry.getOwner(), currentSellEntry.getPos().getX(), currentSellEntry.getPos().getY(), currentSellEntry.getPos().getZ(), currentSellEntry.getSellPricePerItem()));
                i++;
                if (i < currentSellEntries.size())
                    currentSellEntry = currentSellEntries.get(i);
                else break;
            }
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

    private Optional<PlayerWarps.PlayerWarp> getNearestPlayerWarp(Vec3i pos) {
        return EdenClient.getMod(DataFetcher.class).getPlayerWarps().getWarps().stream().min(Comparator.comparingDouble(e -> e.pos().distSqr(pos)));
    }

    private CompletableFuture<Suggestions> suggestSell(CommandContext<FabricClientCommandSource> context, SuggestionsBuilder suggestionsBuilder) {
        ChestShopItemNames itemNameMap = EdenClient.getMod(DataFetcher.class).getChestShopItemNames();
        shops.values().forEach(s -> s.stream().filter(ChestShopEntry::canSell).map(entry -> itemNameMap.getLongName(entry.getItem())).filter(Objects::nonNull).forEach(suggestionsBuilder::suggest));
        return suggestionsBuilder.buildFuture();
    }

    private CompletableFuture<Suggestions> suggestBuy(CommandContext<FabricClientCommandSource> context, SuggestionsBuilder suggestionsBuilder) {
        ChestShopItemNames itemNameMap = EdenClient.getMod(DataFetcher.class).getChestShopItemNames();
        shops.values().forEach(s -> s.stream().filter(ChestShopEntry::canBuy)
                .map(entry -> itemNameMap.getLongName(entry.getItem()))
                .filter(Objects::nonNull).forEach(suggestionsBuilder::suggest));
        return suggestionsBuilder.buildFuture();
    }
}