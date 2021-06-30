package at.haha007.edenclient.mods.chestshop;

import at.haha007.edenclient.callbacks.ConfigLoadCallback;
import at.haha007.edenclient.callbacks.ConfigSaveCallback;
import at.haha007.edenclient.callbacks.PlayerTickCallback;
import at.haha007.edenclient.command.Command;
import at.haha007.edenclient.command.CommandManager;
import at.haha007.edenclient.utils.PlayerUtils;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.text.LiteralText;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.chunk.ChunkManager;
import net.minecraft.world.chunk.WorldChunk;

import java.util.*;
import java.util.stream.Collectors;

public class ChestShopMod {

    Map<ChunkPos, Map<Vec3i, ChestShopEntry>> shops = new HashMap<>();
    private int[] chunk = {0, 0};
    private boolean searchEnabled = true;


    public ChestShopMod() {
        CommandManager.registerCommand(new Command(this::onCommand), "chestshop", "cs");
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
        Map<Vec3i, ChestShopEntry> cs = c.getBlockEntities().values().stream().filter(t -> t instanceof SignBlockEntity).map(t -> (SignBlockEntity) t).
                map(ChestShopEntry::new).filter(ChestShopEntry::isShop).collect(Collectors.toMap(ChestShopEntry::getPos, e -> e));
        shops.put(chunk, cs);
    }

    private void checkForShops(ClientPlayerEntity player) {
        ChunkManager cm = player.clientWorld.getChunkManager();
        ChunkPos.stream(player.getChunkPos(), 5).forEach(cp -> checkForShops(cm, cp));
    }

    public void onCommand(Command command, String s, String[] args) {
        if (args.length == 0) {
            sendMessage("/chestshop sell itemtype");
            sendMessage("/chestshop buy itemtype");
            sendMessage("/chestshop toggle");
            sendMessage("/chestshop clear");
            return;
        }

        StringBuilder sb = new StringBuilder();
        String item;

        switch (args[0].toLowerCase()) {
            case "clear" -> {
                shops.clear();
                sendMessage("Cleared ChestShop entries.");
                return;
            }
            case "list" -> {
                int sum = shops.values().stream().mapToInt(Map::size).sum();
                if (sum < 20)
                    shops.values().forEach(sl -> sl.values().stream().map(cs -> cs.getItem() + " B" + cs.getBuyPricePerItem() + ":" + cs.getSellPricePerItem() + "S").forEach(this::sendMessage));
                sendMessage(String.format("There are %s ChestShops stored.", sum));
                return;
            }
            case "toggle" -> {
                searchEnabled = !searchEnabled;
                sendMessage("ChestShop search " + (searchEnabled ? "enabled" : "disabled"));
                return;
            }
            case "sell" -> {
                if (args.length < 2) break;
                for (int i = 1; i < args.length; i++) {
                    sb.append(" ").append(args[i]);
                }
                item = sb.toString().replaceFirst(" ", "").toLowerCase();
                sendMessage("Sell: ");
                List<ChestShopEntry> matching = new ArrayList<>();
                shops.values().forEach(m -> m.values().stream().filter(ChestShopEntry::canSell).
                        filter(e -> e.getItem().equals(item)).forEach(matching::add));
                matching.stream().sorted(Comparator.comparingDouble(ChestShopEntry::getSellPricePerItem).reversed()).limit(10).map(cs -> String.format(
                        "%s[%d, %d, %d] for %.2f$/item",
                        cs.getOwner(),
                        cs.getPos().getX(),
                        cs.getPos().getY(),
                        cs.getPos().getZ(),
                        cs.getSellPricePerItem())).forEach(this::sendMessage);
                return;
            }
            case "buy" -> {
                if (args.length < 2) break;
                for (int i = 1; i < args.length; i++) {
                    sb.append(" ").append(args[i]);
                }
                item = sb.toString().replaceFirst(" ", "").toLowerCase();
                sendMessage("Buy: ");
                List<ChestShopEntry> matching = new ArrayList<>();
                shops.values().forEach(m -> m.values().stream().filter(ChestShopEntry::canBuy).
                        filter(e -> e.getItem().equals(item)).forEach(matching::add));
                matching.stream().sorted(Comparator.comparingDouble(ChestShopEntry::getBuyPricePerItem)).limit(10).map(cs -> String.format(
                        "%s[%d, %d, %d] for %.2f$/item",
                        cs.getOwner(),
                        cs.getPos().getX(),
                        cs.getPos().getY(),
                        cs.getPos().getZ(),
                        cs.getSellPricePerItem())).forEach(this::sendMessage);
                return;
            }
        }

        sendMessage("/chestshop sell itemtype");
        sendMessage("/chestshop buy itemtype");
        sendMessage("/chestshop toggle");
    }

    private ActionResult saveConfig(NbtCompound overTag) {
        NbtCompound tag = overTag.getCompound("chestshop");
        NbtList list = new NbtList();
        tag.put("entries", list);
        tag.putBoolean("enabled", searchEnabled);
        shops.values().forEach(m -> m.values().forEach(cs -> list.add(cs.toTag())));
        overTag.put("chestshop", tag);
        return ActionResult.PASS;
    }

    private ActionResult loadConfig(NbtCompound overTag) {
        NbtCompound tag = overTag.getCompound("chestshop");
        searchEnabled = !tag.contains("enabled") || tag.getBoolean("enabled");
        NbtList list = tag.getList("entries", 10);
        shops.clear();
        list.stream().map(nbt -> (NbtCompound) nbt).map(ChestShopEntry::new).
                forEach(entry -> shops.put(entry.getChunkPos(), shops.containsKey(entry.getChunkPos()) ?
                        shops.get(entry.getChunkPos()) : new HashMap<>(Map.of(entry.getPos(), entry))));
        return ActionResult.PASS;
    }


    private void sendMessage(String message) {
        PlayerUtils.sendMessage(new LiteralText(message).formatted(Formatting.GOLD));
    }


}
