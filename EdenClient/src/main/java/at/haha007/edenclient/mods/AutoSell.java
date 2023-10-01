package at.haha007.edenclient.mods;

import at.haha007.edenclient.annotations.Mod;
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
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.DefaultedRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;

import java.util.Collections;
import java.util.Comparator;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static at.haha007.edenclient.command.CommandManager.*;
import static at.haha007.edenclient.utils.PlayerUtils.sendModMessage;

@Mod
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
        registerCommand("eas");
    }

    private void registerCommand(String cmd) {
        LiteralArgumentBuilder<ClientSuggestionProvider> node = literal(cmd);

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

        DefaultedRegistry<Item> registry = BuiltInRegistries.ITEM;
        for (Item item : registry) {
            node.then(literal("add").then(literal(registry.getKey(item).toString().replace("minecraft:", "")).executes(c -> {
                autoSellItems.add(item);
                sendModMessage("Added /sell " + item.getDescription().getString());
                return 1;
            })));
        }

        node.then(literal("remove").then(argument("item", StringArgumentType.greedyString()).suggests(this::suggestRemoveItems).executes(c -> {
            Optional<Item> opt = BuiltInRegistries.ITEM.getOptional(new ResourceLocation(c.getArgument("item", String.class).replace(" ", "_")));
            if (opt.isEmpty()) {
                sendModMessage("No item with this name exists.");
                return 1;
            }
            if (autoSellItems.remove(opt.get()))
                sendModMessage("Removed /sell " + opt.get().getDescription().getString());
            else {
                sendModMessage("Couldn't remove /sell " + opt.get().getDescription().getString() + " because it wasn't in your sell list.");
            }
            return 1;
        })));


        node.then(literal("stats").executes(c -> {
            ClientPacketListener networkHandler = Minecraft.getInstance().getConnection();
            if (networkHandler == null) return -1;
            networkHandler.sendChat("/esellstatstracker global");
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
                "AutoSell allows for automatic selling of items in any kind of command-accessible public server shop.",
                "It will always toggle when your inventory is full and execute the sell-command. You can select the items you want to sell yourself.");
    }

    private CompletableFuture<Suggestions> suggestRemoveItems(CommandContext<ClientSuggestionProvider> clientCommandSourceCommandContext, SuggestionsBuilder suggestionsBuilder) {
        DefaultedRegistry<Item> itemRegistry = BuiltInRegistries.ITEM;
        autoSellItems.stream().sorted(Comparator.comparing(s -> s.getDescription().getString()))
                .map(itemRegistry::getKey)
                .map(ResourceLocation::toString)
                .map(itemName -> itemName.split(":")[1])
                .map(String::toLowerCase).toList().forEach(suggestionsBuilder::suggest);
        return suggestionsBuilder.buildFuture();
    }

    public void onInventoryChange(Inventory inventory) {
        if (!enabled || !isFullInventory(inventory)) return;
        executeAutoSell();
    }

    private void executeAutoSell() {
        LocalPlayer player = PlayerUtils.getPlayer();
        ClientPacketListener networkHandler = Minecraft.getInstance().getConnection();
        if(networkHandler == null) return;
        long time = System.currentTimeMillis();
        if (time - 200 < lastSell) return;
        autoSellItems
                .stream()
                .filter(item -> player.getInventory().hasAnyOf(Collections.singleton(item)))
                .forEach(item -> networkHandler.sendChat("/sell " + item.getDescription().getString().replace(' ', '_')));
        lastSell = time;
    }

    private boolean isFullInventory(Inventory inventory) {
        return inventory.getFreeSlot() == -1;
    }
}
