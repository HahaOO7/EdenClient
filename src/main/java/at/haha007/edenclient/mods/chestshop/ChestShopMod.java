package at.haha007.edenclient.mods.chestshop;

import at.haha007.edenclient.callbacks.AddChatMessageCallback;
import at.haha007.edenclient.callbacks.ConfigLoadCallback;
import at.haha007.edenclient.callbacks.ConfigSaveCallback;
import at.haha007.edenclient.callbacks.PlayerTickCallback;
import at.haha007.edenclient.mods.MessageIgnorer;
import at.haha007.edenclient.utils.StringUtils;
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
import net.minecraft.world.chunk.ChunkManager;
import net.minecraft.world.chunk.WorldChunk;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static at.haha007.edenclient.command.CommandManager.*;
import static at.haha007.edenclient.utils.PlayerUtils.sendModMessage;

@SuppressWarnings("AssignmentUsedAsCondition")
public class ChestShopMod {

    Map<ChunkPos, Set<ChestShopEntry>> shops = new HashMap<>();
    BiMap<String, String> originalItemNames = HashBiMap.create();
    private int[] chunk = {0, 0};
    private boolean searchEnabled = true;


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

    private String lastFullNameCached = null;

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

        node.then(literal("mapitemnames").executes(c -> {
            List<String> minecraftIDs = StringUtils.getViableIDs("/minecraft_ids_1.17/minecraft_ids.txt");
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

            if (entityPlayer != null) {
                if (!MessageIgnorer.isEnabled()) {
                    entityPlayer.sendChatMessage("/ignoremessage toggle");
                    entityPlayer.sendChatMessage("Mapping will take up to 25 minutes.");
                }
                new Thread(() -> {
                    {
                        int size = 0;
                        if (minecraftIDs != null) {
                            size = minecraftIDs.size();
                        }

                        if (size == 0) {
                            sendMessage("Error: Size is zero, contact a developer when you encounter this error.");
                        }

                        for (int i = 0; i < size; i++) {
                            try {
                                Thread.sleep(1000);
                                entityPlayer.sendChatMessage("/iteminfo " + minecraftIDs.get(i));
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
            } else {
                sendMessage("Fatal error occurred: entityPlayer is null. If this happens contact a developer.");
            }
            return 1;
        }));

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

    private CompletableFuture<Suggestions> suggestSell(CommandContext<ClientCommandSource> context, SuggestionsBuilder suggestionsBuilder) {
        shops.values().forEach(s -> s.stream().filter(ChestShopEntry::canSell).map(entry -> originalItemNames.get(entry.getItem())).filter(Objects::nonNull).forEach(suggestionsBuilder::suggest));
        return suggestionsBuilder.buildFuture();
    }

    private CompletableFuture<Suggestions> suggestBuy(CommandContext<ClientCommandSource> context, SuggestionsBuilder suggestionsBuilder) {
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
        String mappedNames = tag.getString("mapofnames");
        Arrays.stream(mappedNames.split("~")).map(entry -> entry.split(";")).forEach(entry -> originalItemNames.put(entry[0], entry[1]));
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
