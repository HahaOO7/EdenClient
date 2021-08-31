package at.haha007.edenclient.mods.datafetcher;

import at.haha007.edenclient.EdenClient;
import at.haha007.edenclient.callbacks.AddChatMessageCallback;
import at.haha007.edenclient.mods.MessageIgnorer;
import at.haha007.edenclient.utils.NbtLoadable;
import at.haha007.edenclient.utils.NbtSavable;
import at.haha007.edenclient.utils.PlayerUtils;
import at.haha007.edenclient.utils.Scheduler;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.client.network.ClientCommandSource;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.DefaultedRegistry;
import net.minecraft.util.registry.Registry;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static at.haha007.edenclient.command.CommandManager.literal;
import static at.haha007.edenclient.utils.PlayerUtils.sendModMessage;

public class ChestShopItemNames implements NbtLoadable, NbtSavable {

    private final BiMap<String, String> itemNameMap = HashBiMap.create();
    private String lastFullNameCached = null;
    private boolean nameLookupRunning = false;

    ChestShopItemNames() {
        AddChatMessageCallback.EVENT.register(this::onChat);
    }

    public void load(NbtCompound tag) {
        itemNameMap.clear();
        tag.getKeys().forEach(k -> itemNameMap.put(k, tag.getString(k)));
        nameLookupRunning = false;
    }

    public NbtCompound save() {
        NbtCompound mappedNamesCompound = new NbtCompound();
        itemNameMap.forEach(mappedNamesCompound::putString);
        return mappedNamesCompound;
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
        return itemNameMap.inverse().get(longName);
    }

    public LiteralArgumentBuilder<ClientCommandSource> registerCommand() {
        LiteralArgumentBuilder<ClientCommandSource> mapItemNames = literal("mapitemnames");
        mapItemNames.executes(c -> {
            sendModMessage("/datafetcher mapitemnames <start/check>");
            return 1;
        });

        mapItemNames.then(literal("start").executes(c -> {
            ClientPlayerEntity entityPlayer = PlayerUtils.getPlayer();
            if (entityPlayer == null) return -1;

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
            sendModMessage(new LiteralText("Startet Mapping. Mapping will take about ").formatted(Formatting.GOLD)
                    .append(new LiteralText(Integer.toString(minecraftIDs.length / 60 + 1)).formatted(Formatting.AQUA))
                    .append(new LiteralText(" minutes.").formatted(Formatting.GOLD)));

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
                    sendModMessage(new LiteralText("Mapped ").formatted(Formatting.GOLD)
                            .append(new LiteralText("" + i).formatted(Formatting.AQUA))
                            .append(new LiteralText(" items of ").formatted(Formatting.GOLD))
                            .append(new LiteralText("" + minecraftIDs.length).formatted(Formatting.AQUA))
                            .append(new LiteralText(" this far.").formatted(Formatting.GOLD)));
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
            sendModMessage(new LiteralText("Amount of items mapped: ").formatted(Formatting.GOLD)
                    .append(new LiteralText("" + itemNameMap.size()).formatted(Formatting.AQUA)));
            return 1;
        }));
        return mapItemNames;
    }
}
