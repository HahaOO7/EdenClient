package at.haha007.edenclient.mods.chestshop;

import at.haha007.edenclient.callbacks.ConfigLoadCallback;
import at.haha007.edenclient.callbacks.ConfigSaveCallback;
import at.haha007.edenclient.callbacks.PlayerTickCallback;
import at.haha007.edenclient.command.Command;
import at.haha007.edenclient.command.CommandManager;
import at.haha007.edenclient.utils.PlayerUtils;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.text.LiteralText;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3i;

import java.util.*;

public class ChestShopMod {

    private final Map<Vec3i, ChestShopEntry> shops = new HashMap<>();
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

    private void checkForShops(ClientPlayerEntity player) {
        ClientWorld world = player.clientWorld;
        ChunkPos c = player.getChunkPos();

        shops.keySet().
                stream().
                filter(this::inDistance).
                toList().forEach(shops::remove);

        int dist = 3;
        int minX = c.x - dist;
        int maxX = c.x + dist;
        int minZ = c.z - dist;
        int maxZ = c.z + dist;
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                world.getChunk(x, z).getBlockEntities().values().stream().
                        filter(blockEntity -> blockEntity instanceof SignBlockEntity).
                        map(be -> (SignBlockEntity) be).
                        map(ChestShopEntry::new).
                        filter(ChestShopEntry::isShop).
                        forEach(e -> shops.put(e.getPos(), e));
            }
        }
    }

    private boolean inDistance(Vec3i pos) {
        int x = pos.getX() / 16;
        int z = pos.getZ() / 16;
        int dz = Math.abs(chunk[1] - z);
        int dx = Math.abs(chunk[0] - x);
        return dz <= 3 && dx <= 3;
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
                item = sb.toString().replaceFirst(" ", "");
                sendMessage("Sell: ");
                shops.
                        values().
                        stream().
                        filter(ChestShopEntry::canSell).
                        filter(entry -> entry.getItem().equalsIgnoreCase(item)).
                        sorted((b, a) -> Float.compare(a.getSellPricePerItem(), b.getSellPricePerItem())).
                        limit(10).
                        forEach(cs -> sendMessage(String.format(
                                "%s[%d, %d, %d] for %.2f$/item",
                                cs.getOwner(),
                                cs.getPos().getX(),
                                cs.getPos().getY(),
                                cs.getPos().getZ(),
                                cs.getSellPricePerItem())));
                return;
            }
            case "buy" -> {
                if (args.length < 2) break;
                for (int i = 1; i < args.length; i++) {
                    sb.append(" ").append(args[i]);
                }
                item = sb.toString().replaceFirst(" ", "");
                sendMessage("Buy: ");
                shops.
                        values().
                        stream().
                        filter(ChestShopEntry::canBuy).
                        filter(entry -> entry.getItem().equalsIgnoreCase(item)).
                        sorted((a, b) -> Float.compare(a.getBuyPricePerItem(), b.getBuyPricePerItem())).
                        limit(10).
                        forEach(cs -> sendMessage(String.format(
                                "%s[%d, %d, %d] for %.2f$/item",
                                cs.getOwner(),
                                cs.getPos().getX(),
                                cs.getPos().getY(),
                                cs.getPos().getZ(),
                                cs.getBuyPricePerItem())));
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
        shops.values().forEach(cs -> list.add(cs.toTag()));
        overTag.put("chestshop", tag);
        return ActionResult.PASS;
    }

    private ActionResult loadConfig(NbtCompound overTag) {
        NbtCompound tag = overTag.getCompound("chestshop");
        searchEnabled = !tag.contains("enabled") || tag.getBoolean("enabled");
        NbtList list = tag.getList("entries", 10);
        shops.clear();
        list.stream().map(nbt -> (NbtCompound) nbt).map(ChestShopEntry::new).forEach(entry -> shops.put(entry.getPos(), entry));
        return ActionResult.PASS;
    }

    private void sendMessage(String message) {
        PlayerUtils.sendMessage(new LiteralText(message).formatted(Formatting.GOLD));
    }


}
