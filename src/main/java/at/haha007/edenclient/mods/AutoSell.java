package at.haha007.edenclient.mods;

import at.haha007.edenclient.EdenClient;
import at.haha007.edenclient.callbacks.ConfigLoadCallback;
import at.haha007.edenclient.callbacks.ConfigSaveCallback;
import at.haha007.edenclient.callbacks.PlayerInvChangeCallback;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientCommandSource;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.text.LiteralText;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static at.haha007.edenclient.command.CommandManager.*;
import static at.haha007.edenclient.utils.PlayerUtils.sendModMessage;

public class AutoSell {
    private final Set<Item> autoSellItems = new HashSet<>();
    private long lastSell = 0;
    private boolean enabled;

    public AutoSell() {
        ConfigSaveCallback.EVENT.register(this::onSave);
        ConfigLoadCallback.EVENT.register(this::onLoad);
        PlayerInvChangeCallback.EVENT.register(this::onInventoryChange);
        registerCommand("autosell");
        registerCommand("as");
    }

    private void registerCommand(String cmd) {
        LiteralArgumentBuilder<ClientCommandSource> node = literal(cmd);

        node.then(literal("toggle").executes(c -> {
            enabled = !enabled;
            sendChatMessage(enabled ? "AutoSell enabled" : "AutoSell disabled");
            return 1;
        }));

        node.then(literal("clear").executes(c -> {
            autoSellItems.clear();
            sendChatMessage("Removed all entries");
            return 1;
        }));

        node.then(literal("list").executes(c -> {
            sendChatMessage(autoSellItems.toString());
            return 1;
        }));

        node.then(literal("add").executes(c -> {
            var player = MinecraftClient.getInstance().player;
            if (player == null) return 1;
            PlayerInventory inventory = player.getInventory();
            autoSellItems.add(inventory.getMainHandStack().getItem());
            sendChatMessage("added /sell " + inventory.getMainHandStack().getItem().getName().getString());
            return 1;
        }));

        node.then(literal("remove").executes(c -> {
            var player = MinecraftClient.getInstance().player;
            if (player == null) return 1;
            PlayerInventory inventory = player.getInventory();
            if (autoSellItems.remove(inventory.getMainHandStack().getItem()))
                sendChatMessage("removed /sell " + inventory.getMainHandStack().getItem().getName().getString());
            else {
                sendChatMessage("Couldn't remove /sell " + inventory.getMainHandStack().getItem().getName().getString());
                sendChatMessage("Item was not in the sell list.");
            }
            return 1;
        }));


        node.then(literal("stats").executes(c -> {
            ClientPlayerEntity entityPlayer = MinecraftClient.getInstance().player;
            if (entityPlayer != null) {
                entityPlayer.sendChatMessage("/statstracker global");
            }
            return 1;
        }));

        node.executes(c -> {
            sendChatMessage("/autosell clear");
            sendChatMessage("/autosell list");
            sendChatMessage("/autosell toggle");
            sendChatMessage("/autosell add");
            sendChatMessage("/autosell remove");
            return 1;
        });
        register(node);
    }

    private ActionResult onLoad(NbtCompound compoundTag) {
        NbtCompound tag = compoundTag.getCompound("autoSell");
        enabled = tag.getBoolean("enabled");
        NbtList itemIdentifierList = tag.getList("items", 8);
        autoSellItems.clear();
        for (NbtElement key : itemIdentifierList) {
            Item item = Registry.ITEM.get(new Identifier(key.asString()));
            if (item == Items.AIR) continue;
            autoSellItems.add(item);
        }
        return ActionResult.PASS;
    }

    private ActionResult onSave(NbtCompound compoundTag) {
        NbtCompound tag = compoundTag.getCompound("autoSell");
        List<NbtString> itemIds = autoSellItems.stream().map(item -> Registry.ITEM.getId(item).toString()).map(NbtString::of).collect(Collectors.toList());
        NbtList itemsTag = new NbtList();
        itemsTag.addAll(itemIds);
        tag.put("items", itemsTag);
        tag.putBoolean("enabled", enabled);
        compoundTag.put("autoSell", tag);
        return ActionResult.PASS;
    }


    public ActionResult onInventoryChange(PlayerInventory inventory) {
        if (!enabled || !isFullInventory(inventory)) return ActionResult.PASS;
        executeAutoSell();
        return ActionResult.PASS;
    }

    private void executeAutoSell() {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) return;
        long time = System.currentTimeMillis();
        if (time - 200 < lastSell) return;
        autoSellItems
                .stream()
                .filter(item -> player.getInventory().containsAny(Collections.singleton(item)))
                .forEach(item -> player.sendChatMessage("/sell " + item.getName().getString().replace(' ', '_')));
        lastSell = time;
    }

    private boolean isFullInventory(PlayerInventory inventory) {
        return inventory.getEmptySlot() == -1;
    }


    private void sendChatMessage(String text) {
        sendModMessage(new LiteralText(text).formatted(Formatting.GOLD));
    }
}
