package at.haha007.edenclient.mods.chestshop.pathing;

import at.haha007.edenclient.callbacks.AddChatMessageCallback;
import at.haha007.edenclient.mods.chestshop.ChestShopEntry;
import at.haha007.edenclient.mods.datafetcher.DataFetcher;
import net.minecraft.client.GuiMessage;
import net.minecraft.util.FormattedCharSequence;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChestShopModPathingChatHandler {

    @Language("RegExp")
    private final String OWNER_REGEX = "Owner: (?<owner>[A-Za-z0-9_]{1,16})";

    @Language("RegExp")
    private final String STOCK_REGEX = "Stock: (?<stock>\\d+)";

    @Language("RegExp")
    private final String ITEM_REGEX = "Item: (?<item>.+)";

    @Language("RegExp")
    private final String BUY_REGEX = "Buy (?<amount>\\d+) for (?<price>.+)";

    @Language("RegExp")
    private final String SELL_REGEX = "Sell (?<amount>\\d+) for (?<price>.+)";

    private ChestShopEntry[] sortedEntries;

    private final DataFetcher dataFetcher;

    private int index = 0;

    public ChestShopModPathingChatHandler(ChestShopEntry[] sortedEntries, DataFetcher dataFetcher) {
        this.sortedEntries = sortedEntries;
        this.dataFetcher = dataFetcher;
    }

    public void onChat(AddChatMessageCallback.ChatAddEvent chatAddEvent) {
        String line = chatAddEvent.getChatText().getString();

        ChestShopEntry currentEntry = sortedEntries[index];

        String lastRegex = currentEntry.canSell() ? SELL_REGEX : BUY_REGEX;

        if (line.matches(lastRegex)) {

            String owner = findLatest(OWNER_REGEX, chatAddEvent, "owner")[0];
            String stock = findLatest(STOCK_REGEX, chatAddEvent, "stock")[0];
            String item = findLatest(ITEM_REGEX, chatAddEvent, "item")[0];
            String[] buyResult = findLatest(BUY_REGEX, chatAddEvent, "amount", "price");
            String buyAmount = buyResult != null ? buyResult[0] : null;
            String buyPrice = buyResult != null ? buyResult[1] : null;
            String[] sellResult = findLatest(SELL_REGEX, chatAddEvent, "amount", "price");
            String sellAmount = sellResult != null ? sellResult[0] : null;
            String sellPrice = sellResult != null ? sellResult[1] : null;

            ++index;

            String amount = buyAmount == null ? sellAmount : buyAmount;

            if (!compare(currentEntry, owner, item, amount, buyPrice, sellPrice)) {
                throw new IllegalStateException("Not equal: " + currentEntry + " " + owner + " " + item + " " + stock + " " + Arrays.toString(buyResult) + " " + Arrays.toString(sellResult));
            }

            System.out.println("Shop stock stored for shop at: " + currentEntry.getPos());

            currentEntry.setStock(Integer.parseInt(stock));
        }
    }

    private boolean compare(ChestShopEntry currentEntry, String owner, String item, String amount, String buyPrice, String sellPrice) {
        if (!owner.equals(currentEntry.getOwner())) {
            System.out.println("Owner not equal " + owner + " " + currentEntry);
            return false;
        }
        String shortName = dataFetcher.getChestShopItemNames().getShortName(item);
        if (shortName != null && !shortName.equals(currentEntry.getItem())) {
            System.out.println("Item not equal " + shortName + " " + currentEntry);
            return false;
        }
        if (!amount.equals("" + currentEntry.getAmount())) {
            System.out.println("Amount not equal " + amount + " " + currentEntry);
            return false;
        }
        if (currentEntry.canBuy()) {
            if (Math.abs(Double.parseDouble(buyPrice) - currentEntry.getFullBuyPrice()) > 0.5 || Math.abs(currentEntry.getFullBuyPrice() - Double.parseDouble(buyPrice)) > 0.5) {
                System.out.println("Buy price not equal " + buyPrice + " " + currentEntry.getFullBuyPrice());
                return false;
            }
        } else {
            if (buyPrice != null) {
                System.out.println("Shop is not buy shop, but buyPrice is not null.");
                return false;
            }
        }
        if (currentEntry.canSell()) {
            if (Math.abs(Double.parseDouble(sellPrice) - currentEntry.getFullSellPrice()) > 0.5 || Math.abs(currentEntry.getFullSellPrice() - Double.parseDouble(sellPrice)) > 0.5) {
                System.out.println("Sell price not equal " + sellPrice + " " + currentEntry.getFullSellPrice());
                return false;
            }
        } else {
            if (sellPrice != null) {
                System.out.println("Shop is not sell shop, but sellPrice is not null.");
                return false;
            }
        }
        return true;
    }

    private String[] findLatest(String regex, AddChatMessageCallback.ChatAddEvent event, String... capturingGroups) {
        List<String> rawMessages = getStrings(event);

        for (String rawMessage : rawMessages) {
            if (rawMessage.matches(regex)) {
                String[] results = new String[capturingGroups.length];
                Matcher matcher = Pattern.compile(regex).matcher(rawMessage);
                if (!matcher.find()) {
                    throw new IllegalStateException(); // impossible
                }
                for (int index = 0; index < capturingGroups.length; index++) {
                    String group = capturingGroups[index];
                    results[index] = matcher.group(group);
                }
                return results;
            }
        }

        return null;
    }

    @NotNull
    private static List<String> getStrings(AddChatMessageCallback.ChatAddEvent event) {
        List<String> rawMessages = new ArrayList<>();
        String mainText = event.getChatText().getString();
        rawMessages.add(mainText);

        for (GuiMessage.Line line : event.getChatLines()) {
            FormattedCharSequence lineSequence = line.content();

            StringBuilder stringBuilder = new StringBuilder();

            lineSequence.accept((index, style, codePoint) -> {
                // Assuming style codes are not part of the sequence
                stringBuilder.appendCodePoint(codePoint);
                return true; // continue processing
            });

            String result = stringBuilder.toString();
            rawMessages.add(result);
        }

        return rawMessages;
    }
}
