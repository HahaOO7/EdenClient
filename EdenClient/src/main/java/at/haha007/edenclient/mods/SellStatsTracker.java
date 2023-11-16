package at.haha007.edenclient.mods;

import at.haha007.edenclient.EdenClient;
import at.haha007.edenclient.annotations.Mod;
import at.haha007.edenclient.callbacks.AddChatMessageCallback;
import at.haha007.edenclient.utils.ChatColor;
import at.haha007.edenclient.utils.config.ConfigSubscriber;
import at.haha007.edenclient.utils.config.PerWorldConfig;
import at.haha007.edenclient.utils.config.loaders.ConfigLoader;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static at.haha007.edenclient.command.CommandManager.*;
import static at.haha007.edenclient.utils.PlayerUtils.sendModMessage;

@Mod
public class SellStatsTracker {
    private final Pattern messagePattern = Pattern.compile("Verkauft für \\$(?<money>[0-9]{1,5}\\.?[0-9]{0,2}) \\((?<amount>[0-9,]{1,5}) (?<item>[a-zA-z0-9_]{1,30}) Einheiten je \\$[0-9]{1,5}\\.?[0-9]{0,2}\\)");
    private double amountOfMoneyGainedInSession = 0.0;
    private int index = 0;
    @ConfigSubscriber
    private final SellStatsForItemMap data = new SellStatsForItemMap();
    @ConfigSubscriber
    private boolean simplifyMessages = false;
    @ConfigSubscriber
    private int delayInSimplifiedMessages = 5;

    public SellStatsTracker() {
        registerCommand();
        AddChatMessageCallback.EVENT.register(this::onChat);
        PerWorldConfig pwc = PerWorldConfig.get();
        pwc.register(this, "sellStatsTracker");
        pwc.register(new SellStatsForItemLoader(), SellStatsForItem.class);
        pwc.register(new SellStatsForItemMapLoader(), SellStatsForItemMap.class);
    }

    private void onChat(AddChatMessageCallback.ChatAddEvent event) {
        Component component = event.getChatText();
        if (component == null) {
            return;
        }
        String message = component.getString();
        Matcher matcher = messagePattern.matcher(message);

        if (matcher.matches()) {
            String item = matcher.group("item");
            int amount = Integer.parseInt(matcher.group("amount").replace(",", ""));
            double money = Double.parseDouble(matcher.group("money"));

            amountOfMoneyGainedInSession += money;
            index++;
            sendMessage(amountOfMoneyGainedInSession, index);

            if (data.containsKey(item)) {
                SellStatsForItem stats = data.get(item);
                data.put(item, new SellStatsForItem(stats.amountSold + amount, stats.money + money));
            } else {
                data.put(item, new SellStatsForItem(amount, money));
            }
        }

    }


    public void sendMessage(double amountOfMoneyGainedInSession, int index) {
        if (simplifyMessages && (index % delayInSimplifiedMessages == 0)) {
            sendModMessage(ChatColor.GOLD + "Items sold for a total amount of " + ChatColor.AQUA + "$" + String.format("%1$,.2f", amountOfMoneyGainedInSession) + ChatColor.GOLD + " in this session.");
        }
    }

    private void registerCommand() {
        LiteralArgumentBuilder<ClientSuggestionProvider> node = literal("esellstatstracker");

        node.then(literal("global").executes(c -> {
            sendModMessage(ChatColor.GOLD + "Stats: ");
            data.entrySet().stream().sorted((e1, e2) -> Integer.compare(e2.getValue().amountSold, e1.getValue().amountSold)).toList().forEach(entry -> sendModMessage(
                    ChatColor.AQUA + (entry.getKey().substring(0, 1).toUpperCase() + entry.getKey().substring(1)) +
                            ChatColor.GOLD + " sold " +
                            ChatColor.AQUA + entry.getValue().amountSold +
                            ChatColor.GOLD + " times for a total amount of " +
                            ChatColor.AQUA + "$" + String.format("%1$,.2f", entry.getValue().money + 0.002)));
            sendModMessage(ChatColor.GOLD + "Total amount of money gained: " + ChatColor.AQUA + "$" + String.format("%1$,.2f", data.values().stream().mapToDouble(v -> v.money).sum()));
            return 1;
        }));

        node.then(literal("item").then(argument("itemname", StringArgumentType.word()).suggests(this::suggestItems).executes(c -> {
            data.entrySet().stream().filter(e -> e.getKey().equals(c.getArgument("itemname", String.class)))
                    .forEach(entry -> sendModMessage(
                            ChatColor.AQUA + (entry.getKey().substring(0, 1).toUpperCase() + entry.getKey().substring(1)) +
                                    ChatColor.GOLD + " sold " +
                                    ChatColor.AQUA + entry.getValue().amountSold +
                                    ChatColor.GOLD + " times for a total amount of " +
                                    ChatColor.AQUA + "$" + String.format("%1$,.2f", entry.getValue().money + 0.002)));

            if (data.entrySet().stream().noneMatch(e -> e.getKey().equals(c.getArgument("itemname", String.class))))
                sendModMessage(ChatColor.GOLD + "No data found for this item. All allowed inputs will be tab-completed for you.");

            return 1;
        })));

        LiteralArgumentBuilder<ClientSuggestionProvider> simplify = literal("simplifymessages");
        simplify.then(literal("toggle").executes(c -> {
            simplifyMessages = !simplifyMessages;
            MessageIgnorer mi = EdenClient.getMod(MessageIgnorer.class);
            if (simplifyMessages)
                mi.enable(MessageIgnorer.Predefined.SELL);
            else
                mi.disable(MessageIgnorer.Predefined.SELL);
            sendModMessage(ChatColor.GOLD + (simplifyMessages ? "Sell messages will be simplified" : "Sell messages will not be simplified"));
            return 1;
        }));
        simplify.then(literal("delay").then(argument("messagedelay", IntegerArgumentType.integer(1, Integer.MAX_VALUE)).executes(c -> {
            delayInSimplifiedMessages = c.getArgument("messagedelay", Integer.class);
            sendModMessage(ChatColor.GOLD + "Set delay between automatic simplified messages to " + ChatColor.AQUA + delayInSimplifiedMessages);
            return 1;
        })));
        node.then(simplify);

        register(node,
                "SellStatsTracker tracks all your stats when selling in the AdminShop. You may see how much money you have earned from selling items as well as how often you have sold each specific item.",
                "The simplifymessages option replaces the cluttered messages with better stats.");
    }

    private CompletableFuture<Suggestions> suggestItems(CommandContext<ClientSuggestionProvider> clientCommandSourceCommandContext, SuggestionsBuilder suggestionsBuilder) {
        data.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(e -> suggestionsBuilder.suggest(e.getKey()));
        return suggestionsBuilder.buildFuture();
    }

    private record SellStatsForItem(int amountSold, double money) {
    }

    private static class SellStatsForItemLoader implements ConfigLoader<CompoundTag, SellStatsForItem> {

        @NotNull
        public CompoundTag save(@NotNull SellStatsForItem sellStats) {
            CompoundTag tag = new CompoundTag();
            tag.putInt("amountSold", sellStats.amountSold);
            tag.putDouble("money", sellStats.money);
            return tag;
        }

        @NotNull
        public SellStatsForItem load(@NotNull CompoundTag tag) {
            return new SellStatsForItem(tag.getInt("amountSold"), tag.getDouble("money"));
        }

        @NotNull
        public CompoundTag parse(@NotNull String s) {
            return new CompoundTag();
        }
    }

    private static class SellStatsForItemMapLoader implements ConfigLoader<CompoundTag, SellStatsForItemMap> {

        @NotNull
        public CompoundTag save(@NotNull SellStatsForItemMap map) {
            CompoundTag tag = new CompoundTag();
            map.forEach((k, v) -> tag.put(k, PerWorldConfig.get().toNbt(v)));
            return tag;
        }

        @NotNull
        public SellStatsForItemMap load(@NotNull CompoundTag tag) {
            SellStatsForItemMap map = new SellStatsForItemMap();
            for (String key : tag.getAllKeys()) {
                map.put(key, PerWorldConfig.get().toObject(tag.get(key), SellStatsForItem.class));
            }
            return map;
        }

        @NotNull
        public CompoundTag parse(@NotNull String s) {
            return new CompoundTag();
        }
    }

    private static class SellStatsForItemMap extends HashMap<String, SellStatsForItem> {
    }
}
