package at.haha007.edenclient.mods.datafetcher;

import at.haha007.edenclient.EdenClient;
import at.haha007.edenclient.callbacks.AddChatMessageCallback;
import at.haha007.edenclient.callbacks.LeaveWorldCallback;
import at.haha007.edenclient.mods.MessageIgnorer;
import at.haha007.edenclient.utils.ChatColor;
import at.haha007.edenclient.utils.PlayerUtils;
import at.haha007.edenclient.utils.Scheduler;
import at.haha007.edenclient.utils.config.ConfigSubscriber;
import at.haha007.edenclient.utils.config.PerWorldConfig;
import at.haha007.edenclient.utils.config.wrappers.BiStringStringMap;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.client.network.ClientCommandSource;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.DefaultedRegistry;
import net.minecraft.util.registry.Registry;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static at.haha007.edenclient.command.CommandManager.literal;
import static at.haha007.edenclient.utils.PlayerUtils.sendModMessage;

public class ChestShopItemNames {

    @ConfigSubscriber
    private final BiStringStringMap itemNameMap = new BiStringStringMap();

    private String lastFullNameCached = null;
    private boolean nameLookupRunning = false;

    ChestShopItemNames() {
        AddChatMessageCallback.EVENT.register(this::onChat);
        PerWorldConfig.get().register(this, "dataFetcher.chestShopItemNames");
        LeaveWorldCallback.EVENT.register(() -> nameLookupRunning = false);
    }

    private void onChat(AddChatMessageCallback.ChatAddEvent event) {
        String message = event.getChatText().getString();
        String fullNameMessageSyntax = "Voller Name: (?<originalname>[A-Za-z0-9_ ]{1,40})";
        String shortenedNameMessageSyntax = "Shop Schild: (?<shortenedname>[A-Za-z0-9_ ]{1,40})";

        Matcher fullNameMatcher = Pattern.compile(fullNameMessageSyntax).matcher(message);
        Matcher shortenedNameMatcher = Pattern.compile(shortenedNameMessageSyntax).matcher(message);

        if (fullNameMatcher.matches()) {
            lastFullNameCached = fullNameMatcher.group("originalname").trim().toLowerCase().replace(' ', '_');
        }

        if (lastFullNameCached != null && shortenedNameMatcher.matches()) {
            itemNameMap.put(shortenedNameMatcher.group("shortenedname").toLowerCase(), lastFullNameCached);
            System.out.println("Item mapped: " + lastFullNameCached);
            lastFullNameCached = null;
        }

    }

    public String getLongName(String shortName) {
        return itemNameMap.get(shortName);
    }

    public String getShortName(String longName) {
        return itemNameMap.getKey(longName);
    }

    public LiteralArgumentBuilder<ClientCommandSource> registerCommand() {
        LiteralArgumentBuilder<ClientCommandSource> mapItemNames = literal("mapitemnames");
        mapItemNames.executes(c -> {
            sendModMessage("/datafetcher mapitemnames <start/check>");
            return 1;
        });

        mapItemNames.then(literal("start").executes(c -> {
            ClientPlayerEntity entityPlayer = PlayerUtils.getPlayer();

            if (nameLookupRunning) {
                sendModMessage("Mapping of item names already running!");
                return -1;
            }

            MessageIgnorer mi = EdenClient.INSTANCE.getMessageIgnorer();
            mi.enable(MessageIgnorer.Predefined.ITEM_INFO);

            boolean wasMessageIgnoringEnabled = mi.isEnabled();
            mi.setEnabled(true);


            DefaultedRegistry<Item> itemRegistry = Registry.ITEM;
            String[] minecraftIDs = itemRegistry.stream()
                    .map(itemRegistry::getId)
                    .map(Identifier::toString)
                    .map(itemName -> itemName.split(":")[1])
                    .map(itemName -> itemName.replace('_', ' '))
                    .map(String::toLowerCase)
                    .filter(Predicate.not(itemNameMap::containsValue))
                    .toList().toArray(new String[0]);
            sendModMessage(ChatColor.GOLD + "Started Mapping. Mapping will take about " + ChatColor.AQUA + (minecraftIDs.length / 60 + 1) + " minutes");

            AtomicInteger index = new AtomicInteger();
            nameLookupRunning = true;
            Scheduler.get().scheduleSyncRepeating(() -> {
                int i = index.getAndIncrement();
                if (i >= minecraftIDs.length) {
                    sendModMessage("Finished mapping of all items! Disconnect from the world now to save all items into the config properly! They will be loaded the next time you join the world.");
                    Scheduler.get().scheduleSyncDelayed(() -> {
                        nameLookupRunning = false;
                        mi.disable(MessageIgnorer.Predefined.ITEM_INFO);
                        mi.setEnabled(wasMessageIgnoringEnabled);
                    }, 50);
                    return false;
                }
                String item = minecraftIDs[i];
                System.out.println("Mapping item:" + item);
                entityPlayer.sendChatMessage("/iteminfo " + item);
                if (i % 60 == 0) {
                    sendModMessage(ChatColor.GOLD + "Mapped " + ChatColor.AQUA + i + ChatColor.GOLD + " items of " + ChatColor.AQUA + minecraftIDs.length + ChatColor.GOLD + " this far.");
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
            sendModMessage(ChatColor.GOLD + "Amount of items mapped: " + ChatColor.AQUA + itemNameMap.size());
            return 1;
        }));
        return mapItemNames;
    }
}
