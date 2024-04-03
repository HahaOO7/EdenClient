package at.haha007.edenclient.mods.datafetcher;

import at.haha007.edenclient.EdenClient;
import at.haha007.edenclient.callbacks.AddChatMessageCallback;
import at.haha007.edenclient.callbacks.JoinWorldCallback;
import at.haha007.edenclient.callbacks.LeaveWorldCallback;
import at.haha007.edenclient.mods.MessageIgnorer;
import at.haha007.edenclient.utils.ChatColor;
import at.haha007.edenclient.utils.PlayerUtils;
import at.haha007.edenclient.utils.Scheduler;
import at.haha007.edenclient.utils.Utils;
import at.haha007.edenclient.utils.config.ConfigSubscriber;
import at.haha007.edenclient.utils.config.PerWorldConfig;
import at.haha007.edenclient.utils.config.wrappers.BiStringStringMap;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.kyori.adventure.platform.fabric.FabricAudiences;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.DefaultedRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.PlainTextContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static at.haha007.edenclient.command.CommandManager.argument;
import static at.haha007.edenclient.command.CommandManager.literal;
import static at.haha007.edenclient.utils.PlayerUtils.sendModMessage;

public class ChestShopItemNames {

    @ConfigSubscriber
    private final BiStringStringMap itemNameMap = new BiStringStringMap();
    @ConfigSubscriber("iteminfo")
    private String command = "iteminfo";
    @ConfigSubscriber("20")
    private int fetchDelay = 20;
    @ConfigSubscriber("Full Name:")
    private String fullNamePrefix = "Full Name:";
    @ConfigSubscriber("Shop Sign:")
    private String shortNamePrefix = "Shop Sign:";

    private String lastFullNameCached = null;
    private boolean nameLookupRunning = false;

    ChestShopItemNames() {
        AddChatMessageCallback.EVENT.register(this::onChat);
        PerWorldConfig.get().register(this, "dataFetcher.chestShopItemNames");
        JoinWorldCallback.EVENT.register(() -> nameLookupRunning = false);
        LeaveWorldCallback.EVENT.register(() -> nameLookupRunning = false);
    }

    private void onChat(AddChatMessageCallback.ChatAddEvent event) {
        String message = event.getChatText().getString();

        Matcher fullNameMatcher = Pattern.compile(fullNamePrefix + " (?<originalname>[A-Za-z0-9_ ]{1,40})").matcher(message);
        Matcher shortenedNameMatcher = Pattern.compile(shortNamePrefix + " (?<shortenedname>[A-Za-z0-9_ ]{1,40})").matcher(message);

        if (fullNameMatcher.matches()) {
            lastFullNameCached = fullNameMatcher.group("originalname").trim().toLowerCase().replace(' ', '_');
            return;
        }

        if (lastFullNameCached != null && shortenedNameMatcher.matches()) {
            itemNameMap.put(shortenedNameMatcher.group("shortenedname").toLowerCase(), lastFullNameCached);
            lastFullNameCached = null;
        }
    }

    public String getLongName(String shortName) {
        return itemNameMap.get(shortName);
    }

    public String getShortName(String longName) {
        return itemNameMap.getKey(longName);
    }

    public LiteralArgumentBuilder<ClientSuggestionProvider> registerCommand() {
        LiteralArgumentBuilder<ClientSuggestionProvider> mapItemNames = literal("mapitemnames");
        mapItemNames.executes(c -> {
            sendModMessage("/datafetcher mapitemnames <start/check>");
            return 1;
        });

        mapItemNames.then(literal("start").executes(this::executeFetchCommand));

        mapItemNames.then(literal("delay").executes(c -> {
            PlayerUtils.sendModMessage("Delay: " + fetchDelay);
            return 1;
        }).then(argument("ticks", IntegerArgumentType.integer(1, Integer.MAX_VALUE)).executes(c -> {
            fetchDelay = IntegerArgumentType.getInteger(c, "ticks");
            PlayerUtils.sendModMessage("Set delay to: " + fetchDelay);
            return 1;
        })));

        mapItemNames.then(literal("fetchcommand").executes(c -> {
            PlayerUtils.sendModMessage("Fetch command: " + command);
            PlayerUtils.sendModMessage(Component.literal("Use ").append(Component.literal("/datafetcher " + command).withStyle(ChatFormatting.LIGHT_PURPLE)).append(Component.literal(" for more info")));
            return 1;
        }).then(argument("command", StringArgumentType.word()).executes(c -> {
            command = StringArgumentType.getString(c, "command");
            PlayerUtils.sendModMessage("Set fetch command to: " + command);
            return 1;
        })));

        mapItemNames.then(literal("fullpattern").executes(c -> {
            PlayerUtils.sendModMessage("Full name pattern: " + fullNamePrefix);
            return 1;
        }).then(argument("pattern", StringArgumentType.greedyString()).executes(c -> {
            fullNamePrefix = StringArgumentType.getString(c, "pattern");
            PlayerUtils.sendModMessage("Set full name pattern to: " + fullNamePrefix);
            return 1;
        })));

        mapItemNames.then(literal("shortpattern").executes(c -> {
            PlayerUtils.sendModMessage("Short name pattern: " + shortNamePrefix);
            return 1;
        }).then(argument("pattern", StringArgumentType.greedyString()).executes(c -> {
            shortNamePrefix = StringArgumentType.getString(c, "pattern");
            PlayerUtils.sendModMessage("Set short name pattern to: " + shortNamePrefix);
            return 1;
        })));

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

    private int executeFetchCommand(CommandContext<ClientSuggestionProvider> clientSuggestionProviderCommandContext) {
        LocalPlayer entityPlayer = PlayerUtils.getPlayer();

        if (nameLookupRunning) {
            sendModMessage("Mapping of item names already running!");
            return -1;
        }

        MessageIgnorer mi = EdenClient.getMod(MessageIgnorer.class);
        mi.enable(MessageIgnorer.Predefined.ITEM_INFO);

        boolean wasMessageIgnoringEnabled = mi.isEnabled();
        mi.setEnabled(true);


        DefaultedRegistry<Item> itemRegistry = BuiltInRegistries.ITEM;
        String[] minecraftIDs = itemRegistry.stream().map(itemRegistry::getKey).map(ResourceLocation::toString).map(itemName -> itemName.split(":")[1]).map(itemName -> itemName.replace('_', ' ')).map(String::toLowerCase).filter(Predicate.not(itemNameMap::containsValue)).toList().toArray(new String[0]);
        sendModMessage(ChatColor.GOLD + "Started Mapping. Mapping will take about " + ChatColor.AQUA + (minecraftIDs.length / 60 + 1) + " minutes");

        AtomicInteger index = new AtomicInteger();
        nameLookupRunning = true;
        EdenClient.getMod(Scheduler.class).scheduleSyncRepeating(() -> {
            int i = index.getAndIncrement();
            if (i >= minecraftIDs.length) {
                sendModMessage("Finished mapping of all items! Disconnect from the world now to save all items into the config properly! They will be loaded the next time you join the world.");
                EdenClient.getMod(Scheduler.class).scheduleSyncDelayed(() -> {
                    nameLookupRunning = false;
                    mi.disable(MessageIgnorer.Predefined.ITEM_INFO);
                    mi.setEnabled(wasMessageIgnoringEnabled);
                }, 50);
                return false;
            }
            String item = minecraftIDs[i];
            entityPlayer.connection.sendCommand(command + " " + item);
            if (i % 60 == 0) {
                sendModMessage(ChatColor.GOLD + "Mapped " + ChatColor.AQUA + i + ChatColor.GOLD + " items of " + ChatColor.AQUA + minecraftIDs.length + ChatColor.GOLD + " this far.");
            }
            return true;
        }, fetchDelay, 0);
        return 1;
    }
}
