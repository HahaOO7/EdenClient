package at.haha007.edenclient.mods.chestshop;

import at.haha007.edenclient.EdenClient;
import at.haha007.edenclient.callbacks.AddChatMessageCallback;
import at.haha007.edenclient.callbacks.ConfigLoadCallback;
import at.haha007.edenclient.callbacks.ConfigSaveCallback;
import at.haha007.edenclient.callbacks.PlayerTickCallback;
import at.haha007.edenclient.mods.MessageIgnorer;
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
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.text.LiteralText;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.ChunkPos;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static at.haha007.edenclient.command.CommandManager.*;
import static at.haha007.edenclient.utils.PlayerUtils.sendModMessage;

public class ChestShopMod {

    Map<ChunkPos, Set<ChestShopEntry>> shops = new HashMap<>();
    BiMap<String, String> originalItemNames = HashBiMap.create();
    private int[] chunk = {0, 0};
    private boolean searchEnabled = true;
    private String lastFullNameCached = null;

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

        if (shortenedNameMatcher.matches() && lastFullNameCached != null) {
            originalItemNames.put(shortenedNameMatcher.group("shortenedname").toLowerCase(), lastFullNameCached.toLowerCase());
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
            sendMessage("Cleared ChestShop entries.");
            return 1;
        }));

        node.then(literal("list").executes(c -> {
            int sum = shops.values().stream().mapToInt(Set::size).sum();
            if (sum < 20)
                shops.values().forEach(sl -> sl.stream().map(cs -> cs.getItem() + " B" + cs.getBuyPricePerItem() + ":" + cs.getSellPricePerItem() + "S").forEach(this::sendMessage));
            sendMessage(String.format("There are %s ChestShops stored.", sum));
            return 1;
        }));

        node.then(literal("toggle").executes(c -> {
            sendMessage("ChestShop search " + ((searchEnabled = !searchEnabled) ? "enabled" : "disabled"));
            return 1;
        }));

        node.then(literal("sell").then(argument("item", StringArgumentType.greedyString()).suggests(this::suggestSell).executes(c -> {
            sendMessage("Sell: ");
            String item = originalItemNames.inverse().get(c.getArgument("item", String.class));
            List<ChestShopEntry> matching = new ArrayList<>();

            shops.values().forEach(m -> m.stream().filter(ChestShopEntry::canSell).
                    filter(e -> e.getItem().equals(item)).forEach(matching::add));
            matching.stream().sorted(Comparator.comparingDouble(ChestShopEntry::getSellPricePerItem).reversed()).limit(10).map(cs -> String.format(
                    "%s [%d, %d, %d] for %.2f$/item",
                    cs.getOwner(),
                    cs.getPos().getX(),
                    cs.getPos().getY(),
                    cs.getPos().getZ(),
                    cs.getSellPricePerItem())).forEach(this::sendMessage);
            return 1;
        })));

        node.then(literal("buy").then(argument("item", StringArgumentType.greedyString()).suggests(this::suggestBuy).executes(c -> {
            String item = originalItemNames.inverse().get(c.getArgument("item", String.class));
            sendMessage("Buy: ");
            List<ChestShopEntry> matching = new ArrayList<>();
            shops.values().forEach(m -> m.stream().filter(ChestShopEntry::canBuy).
                    filter(e -> e.getItem().equals(item)).forEach(matching::add));
            matching.stream().sorted(Comparator.comparingDouble(ChestShopEntry::getBuyPricePerItem)).limit(10).map(cs -> String.format(
                    "%s [%d, %d, %d] for %.2f$/item",
                    cs.getOwner(),
                    cs.getPos().getX(),
                    cs.getPos().getY(),
                    cs.getPos().getZ(),
                    cs.getBuyPricePerItem())).forEach(this::sendMessage);
            return 1;
        })));

        node.then(literal("finderrors").executes(c -> {
            HashMap<String, List<ChestShopEntry>> buyEntries = new HashMap<>();
            HashMap<String, List<ChestShopEntry>> sellEntries = new HashMap<>();

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

            List<String> foundDisparities = new ArrayList<>();

            for (Map.Entry<String, List<ChestShopEntry>> entry : buyEntries.entrySet().stream().sorted(Map.Entry.comparingByKey()).collect(Collectors.toList())) {
                if (!sellEntries.containsKey(entry.getKey())) continue;
                List<ChestShopEntry> currentSellEntries = sellEntries.get(entry.getKey()).stream().sorted(Comparator.comparingDouble(ChestShopEntry::getSellPricePerItem).reversed()).collect(Collectors.toList());
                List<ChestShopEntry> currentBuyEntries = entry.getValue().stream().sorted(Comparator.comparingDouble(ChestShopEntry::getBuyPricePerItem)).collect(Collectors.toList());


                ChestShopEntry currentSellEntry = currentSellEntries.get(0);
                ChestShopEntry currentBuyEntry = currentBuyEntries.get(0);
                int i = 0;

                if (currentSellEntry.getSellPricePerItem() <= currentBuyEntry.getBuyPricePerItem())
                    continue;

                String nameOfItem = originalItemNames.get(entry.getKey());
                foundDisparities.add(nameOfItem + ":");

                while (currentSellEntry.getSellPricePerItem() > currentBuyEntry.getBuyPricePerItem()) {
                    foundDisparities.add(String.format("Buy %s at %s [%d, %d, %d] for %.2f$/item and sell at %s [%d, %d, %d] for %.2f$/item",
                            nameOfItem, currentBuyEntry.getOwner(), currentBuyEntry.getPos().getX(), currentBuyEntry.getPos().getY(), currentBuyEntry.getPos().getZ(), currentBuyEntry.getBuyPricePerItem(),
                            currentSellEntry.getOwner(), currentSellEntry.getPos().getX(), currentSellEntry.getPos().getY(), currentSellEntry.getPos().getZ(), currentSellEntry.getSellPricePerItem()));
                    i++;
                    if (i < currentSellEntries.size())
                        currentSellEntry = currentSellEntries.get(i);
                    else break;
                }

                foundDisparities.add("");
            }

            File folder = new File(EdenClient.getDataFolder(), "ChestShopModErrors");
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
                for (String foundDisparity : foundDisparities) {
                    bw.write(foundDisparity);
                    bw.newLine();
                }
                sendModMessage("Wrote file without errors. Saved at /appdata/roaming/.minecraft/config/ChestShopModErrors/[date].txt");
            } catch (IOException e) {
                sendModMessage("Error while writing file. See console for more info.");
                e.printStackTrace();
            }
            return 1;
        }));

        node.then(literal("writeshopstofile").executes(c -> {
            HashMap<String, List<ChestShopEntry>> buyEntries = new HashMap<>();
            HashMap<String, List<ChestShopEntry>> sellEntries = new HashMap<>();

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

            List<String> lines = new ArrayList<>();
            List<String> usedKeys = new ArrayList<>();

            for (Map.Entry<String, List<ChestShopEntry>> entry : buyEntries.entrySet().stream().sorted(Map.Entry.comparingByKey()).collect(Collectors.toList())) {
                List<ChestShopEntry> currentSellEntries = new ArrayList<>();
                if (sellEntries.get(entry.getKey()) != null)
                    currentSellEntries = sellEntries.get(entry.getKey()).stream().sorted(Comparator.comparingDouble(ChestShopEntry::getSellPricePerItem).reversed()).collect(Collectors.toList());
                List<ChestShopEntry> currentBuyEntries = entry.getValue().stream().sorted(Comparator.comparingDouble(ChestShopEntry::getBuyPricePerItem)).collect(Collectors.toList());

                usedKeys.add(entry.getKey());

                String originalName = originalItemNames.get(entry.getKey());
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

                String originalName = originalItemNames.get(entry.getKey());
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

        node.then(literal("mapitemnames").executes(c -> {
            sendModMessage("/chestshop mapitemnames <start/check>");
            return 1;
        }));

        node.then(literal("mapitemnames").then(literal("start").executes(c -> {
            List<String> minecraftIDs = new ArrayList<>();
            Registry.ITEM.forEach(item -> minecraftIDs.add(item.getName().getString()));

            ClientPlayerEntity entityPlayer = MinecraftClient.getInstance().player;
            List<String> ignoredRegexes = MessageIgnorer.getRegexes();
            String iteminfoSyntax = "Item Information: ?";
            String iteminfoSyntax2 = "Voller Name: (?<originalname>[A-Za-z0-9_ ]{1,40})";
            String iteminfoSyntax3 = "Shop Schild: (?<shortenedname>[A-Za-z0-9_ ]{1,40})";
            String iteminfoSyntax4 = "\\/iteminfo \\(what's the item in hand\\?\\) ?";
            String iteminfoSyntax5 = "\\/iteminfo log \\(what's the item ID of LOG\\?\\) ?";

            if (!ignoredRegexes.contains(iteminfoSyntax)) ignoredRegexes.add(iteminfoSyntax);
            if (!ignoredRegexes.contains(iteminfoSyntax2)) ignoredRegexes.add(iteminfoSyntax2);
            if (!ignoredRegexes.contains(iteminfoSyntax3)) ignoredRegexes.add(iteminfoSyntax3);
            if (!ignoredRegexes.contains(iteminfoSyntax4)) ignoredRegexes.add(iteminfoSyntax4);
            if (!ignoredRegexes.contains(iteminfoSyntax5)) ignoredRegexes.add(iteminfoSyntax5);

            if (entityPlayer == null) {
                sendMessage("Fatal error occurred: entityPlayer is null. If this happens contact a developer.");
                return 0;
            }

            if (!MessageIgnorer.isEnabled()) {
                entityPlayer.sendChatMessage("/ignoremessage toggle");
            }

            sendMessage("Startet Mapping. Mapping will take up to 25 minutes.");

            new Thread(() -> {
                {
                    int size = minecraftIDs.size();

                    if (size == 0) {
                        sendMessage("Error: Size is zero, contact a developer when you encounter this error.");
                    }

                    for (int i = 0; i < size; i++) {
                        try {
                            Thread.sleep(1000);
                            if (originalItemNames.inverse().get(minecraftIDs.get(i)) == null)
                                entityPlayer.sendChatMessage("/iteminfo " + minecraftIDs.get(i));
                            else continue;
                        } catch (InterruptedException e) {
                            sendMessage("Error: Thread sleep interrupted.");
                        }
                        if (i % 60 == 0) {
                            sendModMessage(new LiteralText("Mapped ").formatted(Formatting.GOLD)
                                    .append(new LiteralText("" + i).formatted(Formatting.AQUA))
                                    .append(new LiteralText(" items of ").formatted(Formatting.GOLD))
                                    .append(new LiteralText("" + size).formatted(Formatting.AQUA))
                                    .append(new LiteralText(" this far.").formatted(Formatting.GOLD)));
                        }
                    }

                    // Needed for server to respond to last query in the loop without the regexes being removed already
                    try {
                        Thread.sleep(2500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    sendMessage("Finished mapping of all items! Disconnect from the world now to save all items into the config properly! They will be loaded the next time you join the world.");

                    ignoredRegexes.remove(iteminfoSyntax);
                    ignoredRegexes.remove(iteminfoSyntax2);
                    ignoredRegexes.remove(iteminfoSyntax3);
                    ignoredRegexes.remove(iteminfoSyntax4);
                    ignoredRegexes.remove(iteminfoSyntax5);
                }
            }).start();

            return 1;
        })));

        node.then(literal("mapitemnames").then(literal("check").executes(c -> {
            sendModMessage(new LiteralText("Amount of items mapped: ").formatted(Formatting.GOLD)
                    .append(new LiteralText("" + originalItemNames.size()).formatted(Formatting.AQUA)));
            return 1;
        })));

        node.executes(c -> {
            sendMessage("/chestshop sell itemtype");
            sendMessage("/chestshop buy itemtype");
            sendMessage("/chestshop toggle");
            sendMessage("/chestshop clear");
            sendMessage("/chestshop list");
            return 1;
        });
        register(node);
    }

    private CompletableFuture<Suggestions> suggestSell
            (CommandContext<ClientCommandSource> context, SuggestionsBuilder suggestionsBuilder) {
        shops.values().forEach(s -> s.stream().filter(ChestShopEntry::canSell).map(entry -> originalItemNames.get(entry.getItem())).filter(Objects::nonNull).forEach(suggestionsBuilder::suggest));
        return suggestionsBuilder.buildFuture();
    }

    private CompletableFuture<Suggestions> suggestBuy
            (CommandContext<ClientCommandSource> context, SuggestionsBuilder suggestionsBuilder) {
        shops.values().forEach(s -> s.stream().filter(ChestShopEntry::canBuy).map(entry -> originalItemNames.get(entry.getItem())).filter(Objects::nonNull).forEach(suggestionsBuilder::suggest));
        return suggestionsBuilder.buildFuture();
    }

    private ActionResult loadConfig(NbtCompound overTag) {
        NbtCompound tag = overTag.getCompound("chestshop");
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
        if (tag.contains("mapofnames")) {
            String mappedNames = tag.getString("mapofnames");
            try {
                Arrays.stream(mappedNames.split("~")).map(entry -> entry.split(";")).forEach(entry -> originalItemNames.put(entry[0], entry[1]));
            } catch (ArrayIndexOutOfBoundsException | NullPointerException e) {
                System.out.println("EDENCLIENT");
                System.out.println(mappedNames);
                System.out.println("EDENCLIENT");
                e.printStackTrace();
            }
        }
        return ActionResult.PASS;
    }

    private ActionResult saveConfig(NbtCompound overTag) {
        NbtCompound tag = overTag.getCompound("chestshop");
        tag.putBoolean("enabled", searchEnabled);
        NbtList list = new NbtList();
        shops.values().forEach(m -> m.forEach(cs -> list.add(cs.toTag())));
        String mappedNames = originalItemNames.entrySet().stream().map(entry -> entry.getKey() + ";" + entry.getValue()).collect(Collectors.joining("~"));
        tag.putString("mapofnames", mappedNames);
        tag.put("entries", list);
        overTag.put("chestshop", tag);
        return ActionResult.PASS;
    }

    private void sendMessage(String message) {
        sendModMessage(new LiteralText(message).formatted(Formatting.GOLD));
    }
}
