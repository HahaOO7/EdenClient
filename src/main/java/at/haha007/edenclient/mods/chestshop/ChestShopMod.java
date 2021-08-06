package at.haha007.edenclient.mods.chestshop;

import at.haha007.edenclient.EdenClient;
import at.haha007.edenclient.callbacks.AddChatMessageCallback;
import at.haha007.edenclient.callbacks.ConfigLoadCallback;
import at.haha007.edenclient.callbacks.ConfigSaveCallback;
import at.haha007.edenclient.callbacks.PlayerTickCallback;
import at.haha007.edenclient.mods.MessageIgnorer;
import at.haha007.edenclient.utils.PlayerUtils;
import at.haha007.edenclient.utils.Scheduler;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientCommandSource;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Style;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.registry.DefaultedRegistry;
import net.minecraft.util.registry.Registry;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static at.haha007.edenclient.command.CommandManager.*;
import static at.haha007.edenclient.utils.PlayerUtils.sendModMessage;

public class ChestShopMod {

    private final Map<ChunkPos, Set<ChestShopEntry>> shops = new HashMap<>();
    private final BiMap<String, String> itemNameMap = HashBiMap.create();
    private int[] chunk = {0, 0};
    private boolean searchEnabled = true;
    private String lastFullNameCached = null;
    private boolean nameLookupRunning = false;

    public ChestShopMod() {
        registerCommand("chestshop");
        registerCommand("cs");
        PlayerTickCallback.EVENT.register(this::tick);
        ConfigLoadCallback.EVENT.register(this::loadConfig);
        ConfigSaveCallback.EVENT.register(this::saveConfig);
        AddChatMessageCallback.EVENT.register(this::onChat);
    }

    private ActionResult tick(ClientPlayerEntity player) {
        int[] chunk = {player.getChunkPos().x, player.getChunkPos().z};
        if (Arrays.equals(this.chunk, chunk)) return ActionResult.PASS;
        if (!searchEnabled) return ActionResult.PASS;
        this.chunk = chunk;
        checkForShops(player);
        return ActionResult.PASS;
    }

    private ActionResult onChat(AddChatMessageCallback.ChatAddEvent event) {
        String message = event.getChatText().getString();
        String fullNameMessageSyntax = "Voller Name: (?<originalname>[A-Za-z0-9_ ]{1,40})";
        String shortenedNameMessageSyntax = "Shop Schild: (?<shortenedname>[A-Za-z0-9_ ]{1,40})";

        Matcher fullNameMatcher = Pattern.compile(fullNameMessageSyntax).matcher(message);
        Matcher shortenedNameMatcher = Pattern.compile(shortenedNameMessageSyntax).matcher(message);

        if (fullNameMatcher.matches()) {
            lastFullNameCached = fullNameMatcher.group("originalname");
        }

        if (lastFullNameCached != null && shortenedNameMatcher.matches()) {
            itemNameMap.put(shortenedNameMatcher.group("shortenedname").toLowerCase(), lastFullNameCached.toLowerCase());
            System.out.println("Item mapped: " + lastFullNameCached);
            lastFullNameCached = null;
        }

        return ActionResult.PASS;
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

    private void checkForShops(ClientPlayerEntity player) {
        ChunkManager cm = player.clientWorld.getChunkManager();
        ChunkPos.stream(player.getChunkPos(), 5).forEach(cp -> checkForShops(cm, cp));
    }

    private void registerCommand(String name) {
        LiteralArgumentBuilder<ClientCommandSource> node = literal(name);

        node.then(literal("clear").executes(c -> {
            shops.clear();
            sendModMessage("Cleared ChestShop entries.");
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
            String item = itemNameMap.inverse().get(c.getArgument("item", String.class));
            List<ChestShopEntry> matching = new ArrayList<>();

            shops.values().forEach(m -> m.stream().filter(ChestShopEntry::canSell).
                    filter(e -> e.getItem().equals(item)).forEach(matching::add));
            matching.stream().sorted(Comparator.comparingDouble(ChestShopEntry::getSellPricePerItem).reversed()).limit(10).map(cs -> String.format(
                    "%s [%d, %d, %d] for %.2f$/item",
                    cs.getOwner(),
                    cs.getPos().getX(),
                    cs.getPos().getY(),
                    cs.getPos().getZ(),
                    cs.getSellPricePerItem())).forEach(PlayerUtils::sendModMessage);
            return 1;
        })));

        node.then(literal("buy").then(argument("item", StringArgumentType.greedyString()).suggests(this::suggestBuy).executes(c -> {
            String item = itemNameMap.inverse().get(c.getArgument("item", String.class));
            sendModMessage("Buy: ");
            List<ChestShopEntry> matching = new ArrayList<>();
            shops.values().forEach(m -> m.stream().filter(ChestShopEntry::canBuy).
                    filter(e -> e.getItem().equals(item)).forEach(matching::add));
            matching.stream().sorted(Comparator.comparingDouble(ChestShopEntry::getBuyPricePerItem)).limit(10).map(cs -> String.format(
                    "%s [%d, %d, %d] for %.2f$/item",
                    cs.getOwner(),
                    cs.getPos().getX(),
                    cs.getPos().getY(),
                    cs.getPos().getZ(),
                    cs.getBuyPricePerItem())).forEach(PlayerUtils::sendModMessage);
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
            List<String> usedKeys = new ArrayList<>();

            for (Map.Entry<String, List<ChestShopEntry>> entry : buyEntries.entrySet().stream().sorted(Map.Entry.comparingByKey()).collect(Collectors.toList())) {
                List<ChestShopEntry> currentSellEntries = new ArrayList<>();
                if (sellEntries.get(entry.getKey()) != null)
                    currentSellEntries = sellEntries.get(entry.getKey()).stream().sorted(Comparator.comparingDouble(ChestShopEntry::getSellPricePerItem).reversed()).collect(Collectors.toList());
                List<ChestShopEntry> currentBuyEntries = entry.getValue().stream().sorted(Comparator.comparingDouble(ChestShopEntry::getBuyPricePerItem)).collect(Collectors.toList());

                usedKeys.add(entry.getKey());

                String originalName = itemNameMap.get(entry.getKey());
                if (originalName == null) originalName = entry.getKey();

                lines.add(originalName + ":");
                lines.add("Buy:");
                currentBuyEntries.forEach(e -> lines.add(String.format("%-15s [%6d, %3d, %6d] for %.2f$/item", e.getOwner(), e.getPos().getX(), e.getPos().getY(), e.getPos().getZ(), e.getBuyPricePerItem())));
                if (currentSellEntries.size() > 0) {
                    lines.add("Sell:");
                    currentSellEntries.forEach(e -> lines.add(String.format("%-15s [%6d, %3d, %6d] for %.2f$/item", e.getOwner(), e.getPos().getX(), e.getPos().getY(), e.getPos().getZ(), e.getSellPricePerItem())));
                }
                lines.add("");
            }

            for (Map.Entry<String, List<ChestShopEntry>> entry : sellEntries.entrySet().stream().sorted(Map.Entry.comparingByKey()).collect(Collectors.toList())) {
                if (usedKeys.contains(entry.getKey())) continue;
                List<ChestShopEntry> currentSellEntries = entry.getValue().stream().sorted(Comparator.comparingDouble(ChestShopEntry::getBuyPricePerItem).reversed()).collect(Collectors.toList());

                String originalName = itemNameMap.get(entry.getKey());
                if (originalName == null) originalName = entry.getKey();

                lines.add(originalName + ":");
                lines.add("Sell:");
                currentSellEntries.forEach(e -> lines.add(String.format("%-15s [%6d, %3d, %6d] for %7.2f$/item", e.getOwner(), e.getPos().getX(), e.getPos().getY(), e.getPos().getZ(), e.getSellPricePerItem())));
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
                sendModMessage("Wrote file without errors. Saved at /appdata/roaming/.minecraft/config/ChestShopModEntries/[date].txt");
            } catch (IOException e) {
                sendModMessage("Error while writing file. See console for more info.");
                e.printStackTrace();
            }
            return 1;
        }));

        LiteralArgumentBuilder<ClientCommandSource> mapItemNames = literal("mapitemnames");
        mapItemNames.executes(c -> {
            sendModMessage("/chestshop mapitemnames <start/check>");
            return 1;
        });

        mapItemNames.then(literal("start").executes(c -> {
            ClientPlayerEntity entityPlayer = MinecraftClient.getInstance().player;
            if (entityPlayer == null) return -1;

            if (nameLookupRunning) {
                sendModMessage("Mapping of item names already running!");
                return -1;
            }

            MessageIgnorer mi = EdenClient.INSTANCE.getMessageIgnorer();
            mi.enable(MessageIgnorer.Predefined.ITEM_INFO);

            boolean wasMessageIgnoringEnabled = mi.isEnabled();
            mi.setEnabled(true);

            sendModMessage("Startet Mapping. Mapping will take up to 25 minutes.");

            DefaultedRegistry<Item> itemRegistry = Registry.ITEM;
            String[] minecraftIDs = itemRegistry.stream()
                    .map(itemRegistry::getId)
                    .map(Identifier::toString)
                    .map(itemName -> itemName.split(":")[1])
                    .map(itemName -> itemName.replace('_', ' '))
                    .map(String::toLowerCase)
                    .toList().toArray(new String[0]);
            AtomicInteger index = new AtomicInteger();
            nameLookupRunning = true;
            Scheduler.get().scheduleSyncRepeating(() -> {
                String item;
                int i;
                do {
                    i = index.getAndIncrement();
                    if (i >= minecraftIDs.length) {
                        sendModMessage("Finished mapping of all items! Disconnect from the world now to save all items into the config properly! They will be loaded the next time you join the world.");
                        Scheduler.get().scheduleSyncDelayed(() -> {
                            nameLookupRunning = false;
                            mi.disable(MessageIgnorer.Predefined.ITEM_INFO);
                            mi.setEnabled(wasMessageIgnoringEnabled);
                        }, 50);
                        return false;
                    }
                    item = minecraftIDs[i];
                } while (itemNameMap.containsValue(item));
                System.out.println("Mapping item:" + item);
                entityPlayer.sendChatMessage("/iteminfo " + item);
                if (i % 60 == 0) {
                    sendModMessage(new LiteralText("Mapped ").formatted(Formatting.GOLD)
                            .append(new LiteralText("" + i).formatted(Formatting.AQUA))
                            .append(new LiteralText(" items of ").formatted(Formatting.GOLD))
                            .append(new LiteralText("" + minecraftIDs.length).formatted(Formatting.AQUA))
                            .append(new LiteralText(" this far.").formatted(Formatting.GOLD)));
                }
                return true;
            }, 20, 0);
            return 1;
        }));
        mapItemNames.then(literal("reset").executes(c -> {
            sendModMessage("Mapped item names cleared.");
            itemNameMap.clear();
            return 1;
        }));

        mapItemNames.then(literal("check").executes(c -> {
            sendModMessage(new LiteralText("Amount of items mapped: ").formatted(Formatting.GOLD)
                    .append(new LiteralText("" + itemNameMap.size()).formatted(Formatting.AQUA)));
            return 1;
        }));
        node.then(mapItemNames);

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

    private List<String> getExploitableShopsText() {
        Map<String, List<ChestShopEntry>> buyEntries = getBuyShops();
        Map<String, List<ChestShopEntry>> sellEntries = getSellShops();
        List<String> exploitableShopsText = new ArrayList<>();

        for (Map.Entry<String, List<ChestShopEntry>> entry : buyEntries.entrySet().stream().sorted(Map.Entry.comparingByKey()).collect(Collectors.toList())) {
            if (!sellEntries.containsKey(entry.getKey())) continue;
            List<ChestShopEntry> currentSellEntries = sellEntries.get(entry.getKey()).stream().sorted(Comparator.comparingDouble(ChestShopEntry::getSellPricePerItem).reversed()).collect(Collectors.toList());
            List<ChestShopEntry> currentBuyEntries = entry.getValue().stream().sorted(Comparator.comparingDouble(ChestShopEntry::getBuyPricePerItem)).collect(Collectors.toList());

            ChestShopEntry currentSellEntry = currentSellEntries.get(0);
            ChestShopEntry currentBuyEntry = currentBuyEntries.get(0);
            int i = 0;

            if (currentSellEntry.getSellPricePerItem() <= currentBuyEntry.getBuyPricePerItem())
                continue;

            String nameOfItem = itemNameMap.get(entry.getKey());
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

    private CompletableFuture<Suggestions> suggestSell
            (CommandContext<ClientCommandSource> context, SuggestionsBuilder suggestionsBuilder) {
        shops.values().forEach(s -> s.stream().filter(ChestShopEntry::canSell).map(entry -> itemNameMap.get(entry.getItem())).filter(Objects::nonNull).forEach(suggestionsBuilder::suggest));
        return suggestionsBuilder.buildFuture();
    }

    private CompletableFuture<Suggestions> suggestBuy
            (CommandContext<ClientCommandSource> context, SuggestionsBuilder suggestionsBuilder) {
        shops.values().forEach(s -> s.stream().filter(ChestShopEntry::canBuy).map(entry -> itemNameMap.get(entry.getItem())).filter(Objects::nonNull).forEach(suggestionsBuilder::suggest));
        return suggestionsBuilder.buildFuture();
    }

    private ActionResult loadConfig(NbtCompound overTag) {
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
        itemNameMap.clear();
        NbtCompound mappedNamesCompound = tag.getCompound("itemNames");
        mappedNamesCompound.getKeys().forEach(k -> itemNameMap.put(k, mappedNamesCompound.getString(k)));
        nameLookupRunning = false;
        return ActionResult.PASS;
    }

    private ActionResult saveConfig(NbtCompound overTag) {
        NbtCompound tag = overTag.getCompound("chestShop");
        tag.putBoolean("enabled", searchEnabled);
        NbtList list = new NbtList();
        shops.values().forEach(m -> m.forEach(cs -> list.add(cs.toTag())));
        NbtCompound mappedNamesCompound = new NbtCompound();
        itemNameMap.forEach(mappedNamesCompound::putString);
        tag.put("itemNames", mappedNamesCompound);
        tag.put("entries", list);
        overTag.put("chestShop", tag);
        return ActionResult.PASS;
    }

}
