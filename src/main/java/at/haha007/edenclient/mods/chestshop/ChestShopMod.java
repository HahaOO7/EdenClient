package at.haha007.edenclient.mods.chestshop;

import at.haha007.edenclient.EdenClient;
import at.haha007.edenclient.callbacks.ConfigLoadCallback;
import at.haha007.edenclient.callbacks.ConfigSaveCallback;
import at.haha007.edenclient.callbacks.PlayerTickCallback;
import at.haha007.edenclient.mods.datafetcher.ChestShopItemNames;
import at.haha007.edenclient.utils.PlayerUtils;
import at.haha007.edenclient.utils.tasks.RunnableTask;
import at.haha007.edenclient.utils.tasks.TaskManager;
import at.haha007.edenclient.utils.tasks.WaitForTicksTask;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.client.network.ClientCommandSource;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.chunk.ChunkManager;
import net.minecraft.world.chunk.WorldChunk;

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

public class ChestShopMod {

    private final Map<ChunkPos, Set<ChestShopEntry>> shops = new HashMap<>();
    private int[] chunk = {0, 0};
    private boolean searchEnabled = true;

    public ChestShopMod() {
        registerCommand("chestshop");
        registerCommand("cs");
        PlayerTickCallback.EVENT.register(this::tick);
        ConfigLoadCallback.EVENT.register(this::loadConfig);
        ConfigSaveCallback.EVENT.register(this::saveConfig);
    }

    private void tick(ClientPlayerEntity player) {
        int[] chunk = {player.getChunkPos().x, player.getChunkPos().z};
        if (Arrays.equals(this.chunk, chunk)) return;
        if (!searchEnabled) return;
        this.chunk = chunk;
        checkForShops(player);
    }

    private void checkForShops(ChunkManager cm, ChunkPos chunk) {
        if (!cm.isChunkLoaded(chunk.x, chunk.z)) return;
        WorldChunk c = cm.getWorldChunk(chunk.x, chunk.z, false);
        if (c == null) return;

        shops.remove(c.getPos());
        Set<ChestShopEntry> cs = c.getBlockEntities().values().stream().filter(t -> t instanceof SignBlockEntity).map(t -> (SignBlockEntity) t).
                map(ChestShopEntry::new).filter(ChestShopEntry::isShop).collect(Collectors.toSet());
        shops.put(chunk, cs);
    }

    private void checkForShops(ClientPlayerEntity player, int radius) {
        ChunkManager cm = player.clientWorld.getChunkManager();
        ChunkPos.stream(player.getChunkPos(), radius).forEach(cp -> checkForShops(cm, cp));
    }

    private void checkForShops(ClientPlayerEntity player) {
        checkForShops(player, 5);
    }

    private void registerCommand(String name) {
        LiteralArgumentBuilder<ClientCommandSource> node = literal(name);

        node.then(literal("clear").executes(c -> {
            shops.clear();
            sendModMessage("Cleared ChestShop entries.");
            return 1;
        }));

        node.then(literal("updateshops").executes(c -> {
            var shops = EdenClient.INSTANCE.getDataFetcher().getPlayerWarps().getShops();
            TaskManager tm = new TaskManager((shops.size() + 2) * 120);
            sendModMessage(gold("Teleporting to all player warps, this will take about ")
                    .append(aqua(Integer.toString(shops.size() * 6)))
                    .append(gold(" seconds")));
            int count = shops.size();
            AtomicInteger i = new AtomicInteger(1);
            for (String shop : shops.keySet()) {
                tm.then(new RunnableTask(() -> PlayerUtils.messageC2S("/pw " + shop)));
                tm.then(new WaitForTicksTask(120)); //wait for chunks to load
                tm.then(new RunnableTask(() -> sendModMessage(gold("Shop ")
                        .append(aqua((i.getAndIncrement())))
                        .append(gold("/"))
                        .append(aqua(count))
                        .append(gold(" | "))
                        .append(aqua((count - i.get() + 1) * 5))
                        .append(gold(" seconds left")))));
                tm.then(new RunnableTask(() -> checkForShops(PlayerUtils.getPlayer(), 8)));
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
            String item = EdenClient.INSTANCE.getDataFetcher().getChestShopItemNames().getShortName(c.getArgument("item", String.class));
            List<ChestShopEntry> matching = new ArrayList<>();

            shops.values().forEach(m -> m.stream().filter(ChestShopEntry::canSell).
                    filter(e -> e.getItem().equals(item)).forEach(matching::add));
            matching.stream().sorted(Comparator.comparingDouble(ChestShopEntry::getSellPricePerItem).reversed())
                    .limit(10)
                    .map(cs -> {
                        Optional<Map.Entry<String, Vec3i>> opw = getNearestPlayerWarp(cs.getPos());
                        Style style = Style.EMPTY.withColor(Formatting.GOLD);

                        Vec3i pos = cs.getPos();
                        String boxPosStr = pos.getX() + " " + pos.getY() + " " + pos.getZ();
                        String cmd = "/getto " + boxPosStr;
                        Text hoverText = new LiteralText(opw.isPresent() ? opw.get().getKey() : "click me!").formatted(Formatting.GOLD);
                        style = style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hoverText));
                        style = style.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, cmd));
                        return new LiteralText(cs.formattedString(false)).setStyle(style);
                    }).forEach(PlayerUtils::sendModMessage);
            return 1;
        })));

        node.then(literal("buy").then(argument("item", StringArgumentType.greedyString()).suggests(this::suggestBuy).executes(c -> {
            String item = EdenClient.INSTANCE.getDataFetcher().getChestShopItemNames().getShortName(c.getArgument("item", String.class));
            sendModMessage("Buy: ");
            List<ChestShopEntry> matching = new ArrayList<>();
            shops.values().forEach(m -> m.stream().filter(ChestShopEntry::canBuy).
                    filter(e -> e.getItem().equals(item)).forEach(matching::add));
            matching.stream().sorted(Comparator.comparingDouble(ChestShopEntry::getBuyPricePerItem))
                    .limit(10)
                    .map(cs -> {
                        Optional<Map.Entry<String, Vec3i>> opw = getNearestPlayerWarp(cs.getPos());
                        Style style = Style.EMPTY.withColor(Formatting.GOLD);
                        Vec3i pos = cs.getPos();
                        String boxPosStr = pos.getX() + " " + pos.getY() + " " + pos.getZ();
                        String cmd = "/getto " + boxPosStr;
                        Text hoverText = new LiteralText(opw.isPresent() ? opw.get().getKey() : "click me!").formatted(Formatting.GOLD);
                        style = style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hoverText));
                        style = style.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, cmd));
                        return new LiteralText(cs.formattedString(true)).setStyle(style);
                    })
                    .forEach(PlayerUtils::sendModMessage);
            return 1;
        })));

        node.then(literal("exploitable").executes(c -> {
            List<String> exploitableItems = getExploitableShopsText();

            File folder = new File(EdenClient.getDataFolder(), "ChestShop_Exploitable");
            if (!folder.exists()) folder.mkdirs();
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy_MM_dd_hh_mm_ss");
            File file = new File(folder, formatter.format(new Date()) + ".txt");

            try {
                if (!file.exists())
                    if (!file.createNewFile()) return -1;
            } catch (IOException e) {
                e.printStackTrace();
            }

            try (FileWriter writer = new FileWriter(file); BufferedWriter bw = new BufferedWriter(writer)) {
                for (String foundDisparity : exploitableItems) {
                    bw.write(foundDisparity);
                    bw.newLine();
                }
                sendModMessage(new LiteralText("Wrote file without errors. Saved at ").formatted(Formatting.GOLD).
                        append(new LiteralText(file.getAbsolutePath()).setStyle(Style.EMPTY.withColor(Formatting.GOLD)
                                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new LiteralText("Click to copy")))
                                .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, file.getAbsolutePath())))));
            } catch (IOException e) {
                sendModMessage("Error while writing file. See console for more info.");
                e.printStackTrace();
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
            keys = keys.stream().sorted(Comparator.comparing(s -> s)).collect(Collectors.toList());
            ChestShopItemNames itemNameMap = EdenClient.INSTANCE.getDataFetcher().getChestShopItemNames();
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
                if (currentBuyEntries.size() > 0) {
                    lines.add("Buy:");
                    currentBuyEntries.forEach(e -> lines.add(String.format("%-15s [%6d, %3d, %6d] for %.2f$/item", e.getOwner(), e.getPos().getX(), e.getPos().getY(), e.getPos().getZ(), e.getBuyPricePerItem())));
                }
                if (currentSellEntries.size() > 0) {
                    lines.add("Sell:");
                    currentSellEntries.forEach(e -> lines.add(String.format("%-15s [%6d, %3d, %6d] for %.2f$/item", e.getOwner(), e.getPos().getX(), e.getPos().getY(), e.getPos().getZ(), e.getSellPricePerItem())));
                }
                lines.add("");
            }

            File folder = new File(EdenClient.getDataFolder(), "ChestShopModEntries");
            if (!folder.exists()) folder.mkdirs();
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy_MM_dd_hh_mm_ss");
            Date date = new Date();
            File file = new File(folder, formatter.format(date) + ".txt");

            try {
                if (!file.exists())
                    if (!file.createNewFile()) return -1;
            } catch (IOException e) {
                e.printStackTrace();
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
                sendModMessage(new LiteralText("Wrote file without errors. Saved at ").formatted(Formatting.GOLD).
                        append(new LiteralText(file.getAbsolutePath()).setStyle(Style.EMPTY.withColor(Formatting.GOLD)
                                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new LiteralText("Click to copy path")))
                                .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, file.getAbsolutePath())))));
            } catch (IOException e) {
                sendModMessage("Error while writing file. See console for more info.");
                e.printStackTrace();
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
        register(node);
    }

//    private Optional<String> getNearestPlayerWarp(Vec3i pos) {
//        Vec3i pp = PlayerUtils.getPlayer().getBlockPos();
//        return EdenClient.INSTANCE.getDataFetcher().getPlayerWarps().getAll().entrySet().stream()
//                .min(Comparator.comparingDouble(e -> e.getValue().getSquaredDistance(pos)))
//                .map(e -> dist(pos, pp) < dist(e.getValue(), pos) ? null : e.getKey());
//    }
//
//    private double dist(Vec3i a, Vec3i b) {
//        return a.getSquaredDistance(b);
//    }

    private MutableText gold(String string) {
        return new LiteralText(string).formatted(Formatting.GOLD);
    }

    private MutableText aqua(String string) {
        return new LiteralText(string).formatted(Formatting.AQUA);
    }

    private MutableText aqua(int i) {
        return aqua(Integer.toString(i));
    }

    private List<String> getExploitableShopsText() {
        Map<String, List<ChestShopEntry>> buyEntries = getBuyShops();
        Map<String, List<ChestShopEntry>> sellEntries = getSellShops();
        List<String> exploitableShopsText = new ArrayList<>();
        ChestShopItemNames itemNameMap = EdenClient.INSTANCE.getDataFetcher().getChestShopItemNames();
        for (Map.Entry<String, List<ChestShopEntry>> entry : buyEntries.entrySet().stream().sorted(Map.Entry.comparingByKey()).collect(Collectors.toList())) {
            if (!sellEntries.containsKey(entry.getKey())) continue;
            List<ChestShopEntry> currentSellEntries = sellEntries.get(entry.getKey()).stream().sorted(Comparator.comparingDouble(ChestShopEntry::getSellPricePerItem).reversed()).collect(Collectors.toList());
            List<ChestShopEntry> currentBuyEntries = entry.getValue().stream().sorted(Comparator.comparingDouble(ChestShopEntry::getBuyPricePerItem)).collect(Collectors.toList());

            ChestShopEntry currentSellEntry = currentSellEntries.get(0);
            ChestShopEntry currentBuyEntry = currentBuyEntries.get(0);
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

    private Optional<Map.Entry<String, Vec3i>> getNearestPlayerWarp(Vec3i pos) {
        return EdenClient.INSTANCE.getDataFetcher().getPlayerWarps().getAll().entrySet().stream().min(Comparator.comparingDouble(e -> e.getValue().getSquaredDistance(pos)));
    }

    private CompletableFuture<Suggestions> suggestSell(CommandContext<ClientCommandSource> context, SuggestionsBuilder suggestionsBuilder) {
        ChestShopItemNames itemNameMap = EdenClient.INSTANCE.getDataFetcher().getChestShopItemNames();
        shops.values().forEach(s -> s.stream().filter(ChestShopEntry::canSell).map(entry -> itemNameMap.getLongName(entry.getItem())).filter(Objects::nonNull).forEach(suggestionsBuilder::suggest));
        return suggestionsBuilder.buildFuture();
    }

    private CompletableFuture<Suggestions> suggestBuy(CommandContext<ClientCommandSource> context, SuggestionsBuilder suggestionsBuilder) {
        ChestShopItemNames itemNameMap = EdenClient.INSTANCE.getDataFetcher().getChestShopItemNames();
        shops.values().forEach(s -> s.stream().filter(ChestShopEntry::canBuy).map(entry -> itemNameMap.getLongName(entry.getItem())).filter(Objects::nonNull).forEach(suggestionsBuilder::suggest));
        return suggestionsBuilder.buildFuture();
    }

    private void loadConfig(NbtCompound overTag) {
        NbtCompound tag = overTag.getCompound("chestShop");
        searchEnabled = !tag.contains("enabled") || tag.getBoolean("enabled");
        NbtList list = tag.getList("entries", 10);
        shops.clear();
        list.stream().map(nbt -> (NbtCompound) nbt).map(ChestShopEntry::new).
                forEach(entry -> {
                    if (shops.containsKey(entry.getChunkPos()))
                        shops.get(entry.getChunkPos()).add(entry);
                    else
                        shops.put(entry.getChunkPos(), new HashSet<>(Set.of(entry)));
                });
    }

    private void saveConfig(NbtCompound overTag) {
        NbtCompound tag = overTag.getCompound("chestShop");
        tag.putBoolean("enabled", searchEnabled);
        NbtList list = new NbtList();
        shops.values().forEach(m -> m.forEach(cs -> list.add(cs.toTag())));
        tag.put("entries", list);
        overTag.put("chestShop", tag);
    }

}