package at.haha007.edenclient.mods;

import at.haha007.edenclient.EdenClient;
import at.haha007.edenclient.callbacks.AddChatMessageCallback;
import at.haha007.edenclient.utils.config.ConfigSubscriber;
import at.haha007.edenclient.utils.config.PerWorldConfig;
import at.haha007.edenclient.utils.config.loaders.ConfigLoader;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.client.network.ClientCommandSource;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Formatting;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static at.haha007.edenclient.command.CommandManager.*;
import static at.haha007.edenclient.utils.PlayerUtils.sendModMessage;
import static at.haha007.edenclient.utils.TextUtils.createGoldText;

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
        String message = event.getChatText().getString();
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
            sendModMessage(new LiteralText("Items sold for a total amount of ").formatted(Formatting.GOLD).append(new LiteralText("$" + String.format("%1$,.2f", amountOfMoneyGainedInSession)).formatted(Formatting.AQUA)).append(new LiteralText(" in this session.").formatted(Formatting.GOLD)));
        }
    }

    private void registerCommand() {
        LiteralArgumentBuilder<ClientCommandSource> node = literal("esellstatstracker");

        node.then(literal("global").executes(c -> {
            sendModMessage(new LiteralText("Stats: ").formatted(Formatting.GOLD));
            data.entrySet().stream().sorted((e1, e2) -> Integer.compare(e2.getValue().amountSold, e1.getValue().amountSold)).collect(Collectors.toList()).forEach(entry -> sendModMessage(
                    new LiteralText(entry.getKey().substring(0, 1).toUpperCase() + entry.getKey().substring(1)).formatted(Formatting.AQUA)
                            .append(new LiteralText(" sold ").formatted(Formatting.GOLD))
                            .append(new LiteralText("" + entry.getValue().amountSold).formatted(Formatting.AQUA))
                            .append(new LiteralText(" times for a total amount of ").formatted(Formatting.GOLD))
                            .append(new LiteralText("$" + String.format("%1$,.2f", entry.getValue().money + 0.002)).formatted(Formatting.AQUA))));
            sendModMessage(new LiteralText("Total amount of money gained: ").formatted(Formatting.GOLD).append(new LiteralText("$" + String.format("%1$,.2f", data.values().stream().mapToDouble(v -> v.money).sum())).formatted(Formatting.AQUA)))
            ;
            return 1;
        }));

        node.then(literal("item").then(argument("itemname", StringArgumentType.word()).suggests(this::suggestItems).executes(c -> {
            data.entrySet().stream().filter(e -> e.getKey().equals(c.getArgument("itemname", String.class)))
                    .forEach(entry -> sendModMessage(
                            new LiteralText(entry.getKey().substring(0, 1).toUpperCase() + entry.getKey().substring(1)).formatted(Formatting.AQUA)
                                    .append(new LiteralText(" sold ").formatted(Formatting.GOLD))
                                    .append(new LiteralText("" + entry.getValue().amountSold).formatted(Formatting.AQUA))
                                    .append(new LiteralText(" times for a total amount of ").formatted(Formatting.GOLD))
                                    .append(new LiteralText("$" + String.format("%1$,.2f", entry.getValue().money + 0.002)).formatted(Formatting.AQUA))));

            if (data.entrySet().stream().noneMatch(e -> e.getKey().equals(c.getArgument("itemname", String.class))))
                sendModMessage(new LiteralText("No data found for this item. All allowed inputs will be tab-completed for you.").formatted(Formatting.GOLD));

            return 1;
        })));

        LiteralArgumentBuilder<ClientCommandSource> simplify = literal("simplifymessages");
        simplify.then(literal("toggle").executes(c -> {
            simplifyMessages = !simplifyMessages;
            MessageIgnorer mi = EdenClient.INSTANCE.getMessageIgnorer();
            if (simplifyMessages)
                mi.enable(MessageIgnorer.Predefined.SELL);
            else
                mi.disable(MessageIgnorer.Predefined.SELL);
            sendModMessage(new LiteralText(simplifyMessages ? "Sell messages will be simplified" : "Sell messages will not be simplified").formatted(Formatting.GOLD));
            return 1;
        }));
        simplify.then(literal("delay").then(argument("messagedelay", IntegerArgumentType.integer(1, Integer.MAX_VALUE)).executes(c -> {
            delayInSimplifiedMessages = c.getArgument("messagedelay", Integer.class);
            sendModMessage(new LiteralText("Set delay between automatic simplified messages to ").formatted(Formatting.GOLD).append("" + delayInSimplifiedMessages).formatted(Formatting.AQUA));
            return 1;
        })));
        node.then(simplify);

        register(node,
                createGoldText("SellStatsTracker tracks all your stats when selling in the AdminShop. You may see how much money you have earned from selling items as well as how often you have sold each specific item."),
                createGoldText("The simplifymessages option replaces the cluttered messages with better stats."));
    }

    private CompletableFuture<Suggestions> suggestItems(CommandContext<ClientCommandSource> clientCommandSourceCommandContext, SuggestionsBuilder suggestionsBuilder) {
        data.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(e -> suggestionsBuilder.suggest(e.getKey()));
        return suggestionsBuilder.buildFuture();
    }

    private static record SellStatsForItem(int amountSold, double money) {
    }

    private static class SellStatsForItemLoader implements ConfigLoader<NbtCompound, SellStatsForItem> {
        public NbtCompound save(Object value) {
            SellStatsForItem ss = cast(value);
            NbtCompound tag = new NbtCompound();
            tag.putInt("amountSold", ss.amountSold);
            tag.putDouble("money", ss.money);
            return tag;
        }

        public SellStatsForItem load(NbtCompound tag) {
            return new SellStatsForItem(tag.getInt("amountSold"), tag.getDouble("money"));
        }

        public NbtCompound parse(String s) {
            return new NbtCompound();
        }
    }

    private static class SellStatsForItemMapLoader implements ConfigLoader<NbtCompound, SellStatsForItemMap> {
        public NbtCompound save(Object value) {
            SellStatsForItemMap map = cast(value);
            NbtCompound tag = new NbtCompound();
            map.forEach((k, v) -> tag.put(k, PerWorldConfig.get().toNbt(v)));
            return tag;
        }

        public SellStatsForItemMap load(NbtCompound tag) {
            SellStatsForItemMap map = new SellStatsForItemMap();
            for (String key : tag.getKeys()) {
                map.put(key, PerWorldConfig.get().toObject(tag.get(key), SellStatsForItem.class));
            }
            return map;
        }

        public NbtCompound parse(String s) {
            return new NbtCompound();
        }
    }

    private static class SellStatsForItemMap extends HashMap<String, SellStatsForItem> {
    }
}
