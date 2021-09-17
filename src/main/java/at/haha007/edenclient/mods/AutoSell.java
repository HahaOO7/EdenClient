package at.haha007.edenclient.mods;

import at.haha007.edenclient.callbacks.PlayerInvChangeCallback;
import at.haha007.edenclient.utils.PlayerUtils;
import at.haha007.edenclient.utils.config.ConfigSubscriber;
import at.haha007.edenclient.utils.config.PerWorldConfig;
import at.haha007.edenclient.utils.config.wrappers.ItemSet;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.client.network.ClientCommandSource;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.DefaultedRegistry;
import net.minecraft.util.registry.Registry;

import java.util.*;
import java.util.concurrent.CompletableFuture;

import static at.haha007.edenclient.command.CommandManager.*;
import static at.haha007.edenclient.utils.PlayerUtils.sendModMessage;

public class AutoSell {
    @ConfigSubscriber()
    private final ItemSet autoSellItems = new ItemSet();
    private long lastSell = 0;
    @ConfigSubscriber("false")
    private boolean enabled;

    public AutoSell() {
        PerWorldConfig.get().register(this, "autoSell");
        PlayerInvChangeCallback.EVENT.register(this::onInventoryChange);
        registerCommand("eautosell");
    }

    private void registerCommand(String cmd) {
        LiteralArgumentBuilder<ClientCommandSource> node = literal(cmd);

        node.then(literal("toggle").executes(c -> {
            enabled = !enabled;
            sendModMessage(enabled ? "AutoSell enabled" : "AutoSell disabled");
            return 1;
        }));

        node.then(literal("clear").executes(c -> {
            autoSellItems.clear();
            sendModMessage("Removed all entries");
            return 1;
        }));

        node.then(literal("list").executes(c -> {
            sendModMessage(autoSellItems.toString());
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
            Optional<Item> opt = Registry.ITEM.getOrEmpty(new Identifier(c.getArgument("item", String.class).replace(" ", "_")));
            if (opt.isEmpty()) {
                sendModMessage("No item with this name exists.");
                return 1;
            }
            if (autoSellItems.remove(opt.get()))
                sendModMessage("Removed /sell " + opt.get().getName().getString());
            else {
                sendModMessage("Couldn't remove /sell " + opt.get().getName().getString() + " because it wasn't in your sell list.");
            }
            return 1;
        })));


        node.then(literal("stats").executes(c -> {
            ClientPlayerEntity entityPlayer = PlayerUtils.getPlayer();
            entityPlayer.sendChatMessage("/esellstatstracker global");
            return 1;
        }));

        node.executes(c -> {
            sendModMessage("/autosell clear");
            sendModMessage("/autosell list");
            sendModMessage("/autosell toggle");
            sendModMessage("/autosell add");
            sendModMessage("/autosell remove");
            return 1;
        });

        register(node,
                new LiteralText("AutoSell allows for automatic selling of items in any kind of command-accessible public server shop.").formatted(Formatting.GOLD),
                new LiteralText("It will always toggle when your inventory is full and execute the sell-command. You can select the items you want to sell yourself.").formatted(Formatting.GOLD));
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

    public void onInventoryChange(PlayerInventory inventory) {
        if (!enabled || !isFullInventory(inventory)) return;
        executeAutoSell();
    }

    private void executeAutoSell() {
        ClientPlayerEntity player = PlayerUtils.getPlayer();
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
}
