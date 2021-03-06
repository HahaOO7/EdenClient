package at.haha007.edenclient.mods.chestshop;

import at.haha007.edenclient.callbacks.ConfigLoadCallback;
import at.haha007.edenclient.callbacks.ConfigSaveCallback;
import at.haha007.edenclient.callbacks.PlayerTickCallback;
import at.haha007.edenclient.utils.PlayerUtils;
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
import net.minecraft.text.LiteralText;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.ChunkManager;
import net.minecraft.world.chunk.WorldChunk;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static at.haha007.edenclient.command.CommandManager.*;

public class ChestShopMod {

    Map<ChunkPos, Set<ChestShopEntry>> shops = new HashMap<>();
    private int[] chunk = {0, 0};
    private boolean searchEnabled = true;


    public ChestShopMod() {
        registerCommand("chestshop");
        registerCommand("cs");
        PlayerTickCallback.EVENT.register(this::tick);
        ConfigLoadCallback.EVENT.register(this::loadConfig);
        ConfigSaveCallback.EVENT.register(this::saveConfig);
    }

    private ActionResult tick(ClientPlayerEntity player) {
        int[] chunk = {player.getChunkPos().x, player.getChunkPos().z};
        if (Arrays.equals(this.chunk, chunk)) return ActionResult.PASS;
        if (!searchEnabled) return ActionResult.PASS;
        this.chunk = chunk;
        checkForShops(player);
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
            String item = c.getArgument("item", String.class);
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
            String item = c.getArgument("item", String.class);
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
        shops.values().forEach(s -> s.stream().filter(ChestShopEntry::canSell).map(ChestShopEntry::getItem).forEach(suggestionsBuilder::suggest));
        return  suggestionsBuilder.buildFuture();
    }
    private CompletableFuture<Suggestions> suggestBuy(CommandContext<ClientCommandSource> context, SuggestionsBuilder suggestionsBuilder) {
        shops.values().forEach(s -> s.stream().filter(ChestShopEntry::canBuy).map(ChestShopEntry::getItem).forEach(suggestionsBuilder::suggest));
        return  suggestionsBuilder.buildFuture();
    }

    private ActionResult saveConfig(NbtCompound overTag) {
        NbtCompound tag = overTag.getCompound("chestshop");
        tag.putBoolean("enabled", searchEnabled);
        NbtList list = new NbtList();
        shops.values().forEach(m -> m.forEach(cs -> list.add(cs.toTag())));
        tag.put("entries", list);
        overTag.put("chestshop", tag);
        return ActionResult.PASS;
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
        return ActionResult.PASS;
    }

    private void sendMessage(String message) {
        PlayerUtils.sendModMessage(new LiteralText(message).formatted(Formatting.GOLD));
    }


}
