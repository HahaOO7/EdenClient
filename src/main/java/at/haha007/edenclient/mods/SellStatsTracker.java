package at.haha007.edenclient.mods;

import at.haha007.edenclient.callbacks.AddChatMessageCallback;
import at.haha007.edenclient.callbacks.ConfigLoadCallback;
import at.haha007.edenclient.callbacks.ConfigSaveCallback;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.client.network.ClientCommandSource;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.LiteralText;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static at.haha007.edenclient.command.CommandManager.*;
import static at.haha007.edenclient.utils.PlayerUtils.sendModMessage;

public class SellStatsTracker {
    private static record SellStatsForItem(int amountSold, double money) {
    }

    HashMap<String, SellStatsForItem> data = new HashMap<>();
    private static final Pattern messagePattern = Pattern.compile("Verkauft für \\$(?<money>[0-9]{1,5}\\.?[0-9]{0,2}) \\((?<amount>[0-9,]{1,4}) (?<item>[a-zA-z0-9_]{1,30}) Einheiten je \\$[0-9]{1,5}\\.?[0-9]{0,2}\\)");
    private static double amountOfMoneyGainedInSession = 0.0;
    private static int index = 0;

    public SellStatsTracker() {
        registerCommand("sellstatstracker");
        registerCommand("statstracker");
        AddChatMessageCallback.EVENT.register(this::onChat);
        ConfigSaveCallback.EVENT.register(this::onSave);
        ConfigLoadCallback.EVENT.register(this::onLoad);
    }

    private ActionResult onChat(AddChatMessageCallback.ChatAddEvent event) {
        String message = event.getChatText().getString();
        Matcher matcher = messagePattern.matcher(message);

        if (matcher.matches()) {
            String item = matcher.group("item");
            int amount = Integer.parseInt(matcher.group("amount"));
            double money = Double.parseDouble(matcher.group("money"));

            amountOfMoneyGainedInSession += money;
            index++;
            AutoSell.sendMessage(amountOfMoneyGainedInSession, index);

            if (data.containsKey(item)) {
                SellStatsForItem stats = data.get(item);
                data.put(item, new SellStatsForItem(stats.amountSold + amount, stats.money + money));
            } else {
                data.put(item, new SellStatsForItem(amount, money));
            }
        }

        return ActionResult.PASS;
    }

    private void registerCommand(String literal) {
        LiteralArgumentBuilder<ClientCommandSource> node = literal(literal);

        node.then(literal("global").executes(c -> {
            sendModMessage(new LiteralText("Stats: ").formatted(Formatting.GOLD));
            data.entrySet().stream().sorted(Comparator.comparingInt(e -> ((Map.Entry<String, SellStatsForItem>) e).getValue().amountSold).reversed()).collect(Collectors.toList()).forEach(entry -> sendModMessage(
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

        register(node);
    }

    private CompletableFuture<Suggestions> suggestItems(CommandContext<ClientCommandSource> clientCommandSourceCommandContext, SuggestionsBuilder suggestionsBuilder) {
        data.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(e -> suggestionsBuilder.suggest(e.getKey()));
        return suggestionsBuilder.buildFuture();
    }

    private ActionResult onLoad(NbtCompound nbtCompound) {
        if (nbtCompound.contains("sellstatstracker")) {
            NbtCompound tag = nbtCompound.getCompound("sellstatstracker");
            if (tag.contains("data")) {
                List<String[]> data = Arrays.stream(tag.getString("data").split("~")).map(string -> string.split(";")).collect(Collectors.toList());
                for (String[] entry : data) {
                    this.data.put(entry[0], new SellStatsForItem(Integer.parseInt(entry[1]), Double.parseDouble(entry[2])));
                }
            }
            return ActionResult.PASS;
        }
        return ActionResult.PASS;
    }

    private ActionResult onSave(NbtCompound nbtCompound) {
        NbtCompound compound = new NbtCompound();

        String dataString;
        if (!data.values().isEmpty()) {
            dataString = data.entrySet().stream().sorted(Comparator.comparingInt(e -> ((Map.Entry<String, SellStatsForItem>) e).getValue().amountSold).reversed())
                    .map(entry -> entry.getKey() + ";" + entry.getValue().amountSold + ";" + entry.getValue().money).collect(Collectors.joining("~"));
            compound.putString("data", dataString);
        }
        nbtCompound.put("sellstatstracker", compound);
        return ActionResult.PASS;
    }
}
