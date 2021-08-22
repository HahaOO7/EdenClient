package at.haha007.edenclient.mods;

import at.haha007.edenclient.callbacks.ConfigLoadCallback;
import at.haha007.edenclient.callbacks.ConfigSaveCallback;
import at.haha007.edenclient.callbacks.PlayerInvChangeCallback;
import at.haha007.edenclient.utils.PlayerUtils;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
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
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.DefaultedRegistry;
import net.minecraft.util.registry.Registry;

import java.util.*;
import java.util.concurrent.CompletableFuture;
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

        DefaultedRegistry<Item> registry = Registry.ITEM;
        for (Item item : registry) {
            node.then(literal("add").then(literal(registry.getId(item).toString().replace("minecraft:", "")).executes(c -> {
                autoSellItems.add(item);
                sendModMessage("Added /sell " + item.getName().getString());
                return 1;
            })));
        }

        node.then(literal("remove").then(argument("item", StringArgumentType.greedyString()).suggests(this::suggestRemoveItems).executes(c -> {
            ClientPlayerEntity player = PlayerUtils.getPlayer();
            if (player == null) return 1;

            Optional<Item> opt = Registry.ITEM.getOrEmpty(new Identifier(c.getArgument("item", String.class).replace(" ", "_")));
            if (opt.isEmpty()) {
                sendModMessage("No item with this name exists.");
                return 1;
            }
            if (autoSellItems.remove(opt.get()))
                sendChatMessage("Removed /sell " + opt.get().getName().getString());
            else {
                sendChatMessage("Couldn't remove /sell " + opt.get().getName().getString() + " because it wasn't in your sell list.");
            }
            return 1;
        })));


        node.then(literal("stats").executes(c -> {
            ClientPlayerEntity entityPlayer = PlayerUtils.getPlayer();
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

    private CompletableFuture<Suggestions> suggestRemoveItems(CommandContext<ClientCommandSource> clientCommandSourceCommandContext, SuggestionsBuilder suggestionsBuilder) {
        DefaultedRegistry<Item> itemRegistry = Registry.ITEM;
        autoSellItems.stream().sorted(Comparator.comparing(s -> s.getName().getString()))
                .map(itemRegistry::getId)
                .map(Identifier::toString)
                .map(itemName -> itemName.split(":")[1])
                .map(String::toLowerCase).toList().forEach(suggestionsBuilder::suggest);
        return suggestionsBuilder.buildFuture();
    }

    private void onLoad(NbtCompound compoundTag) {
        NbtCompound tag = compoundTag.getCompound("autoSell");
        enabled = tag.getBoolean("enabled");
        NbtList itemIdentifierList = tag.getList("items", 8);
        autoSellItems.clear();
        for (NbtElement key : itemIdentifierList) {
            Item item = Registry.ITEM.get(new Identifier(key.asString()));
            if (item == Items.AIR) continue;
            autoSellItems.add(item);
        }
    }

    private void onSave(NbtCompound compoundTag) {
        NbtCompound tag = compoundTag.getCompound("autoSell");
        List<NbtString> itemIds = autoSellItems.stream().map(item -> Registry.ITEM.getId(item).toString()).map(NbtString::of).collect(Collectors.toList());
        NbtList itemsTag = new NbtList();
        itemsTag.addAll(itemIds);
        tag.put("items", itemsTag);
        tag.putBoolean("enabled", enabled);
        compoundTag.put("autoSell", tag);
    }


    public void onInventoryChange(PlayerInventory inventory) {
        if (!enabled || !isFullInventory(inventory)) return;
        executeAutoSell();
    }

    private void executeAutoSell() {
        ClientPlayerEntity player = PlayerUtils.getPlayer();
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
