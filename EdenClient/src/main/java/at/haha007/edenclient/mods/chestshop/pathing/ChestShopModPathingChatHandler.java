package at.haha007.edenclient.mods.chestshop.pathing;

import at.haha007.edenclient.callbacks.AddChatMessageCallback;
import at.haha007.edenclient.mods.chestshop.ChestShopEntry;
import at.haha007.edenclient.mods.datafetcher.DataFetcher;
import net.minecraft.client.GuiMessage;
import net.minecraft.util.FormattedCharSequence;
import org.intellij.lang.annotations.Language;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static at.haha007.edenclient.utils.PlayerUtils.sendModMessage;

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

            String owner = findLatest(OWNER_REGEX, chatAddEvent.getChatLines(), "owner")[0];
            String stock = findLatest(STOCK_REGEX, chatAddEvent.getChatLines(), "stock")[0];
            String item = findLatest(ITEM_REGEX, chatAddEvent.getChatLines(), "item")[0];
            String[] buyResult = findLatest(BUY_REGEX, chatAddEvent.getChatLines(), "amount", "price");
            String buyAmount = buyResult != null ? buyResult[0] : null;
            String buyPrice = buyResult != null ? buyResult[1] : null;
            String[] sellResult = findLatest(SELL_REGEX, chatAddEvent.getChatLines(), "amount", "price");
            String sellPrice = sellResult != null ? sellResult[1] : null;

            ++index;

            if (!compare(currentEntry, owner, item, buyAmount, buyPrice, sellPrice)) {
                throw new IllegalStateException();
            }

            currentEntry.setStock(Integer.parseInt(stock));
        }

        System.out.println(Arrays.toString(sortedEntries));
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
            if (!buyPrice.equals("" + currentEntry.getFullBuyPrice())) {
                System.out.println("Buy price not equal " + buyPrice + " " + currentEntry);
                return false;
            }
        } else {
            if (buyPrice != null) {
                System.out.println("Shop is not buy shop, but buyPrice is not null.");
                return false;
            }
        }
        if (currentEntry.canSell()) {
            if (!sellPrice.equals("" + currentEntry.getFullSellPrice())) {
                System.out.println("Sell price not equal " + sellPrice + " " + currentEntry);
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

    private String[] findLatest(String regex, List<GuiMessage.Line> lines, String... capturingGroups) {
        for (GuiMessage.Line line : lines) {
            FormattedCharSequence lineSequence = line.content();

            StringBuilder stringBuilder = new StringBuilder();

            lineSequence.accept((index, style, codePoint) -> {
                // Assuming style codes are not part of the sequence
                stringBuilder.appendCodePoint(codePoint);
                return true; // continue processing
            });

            String result = stringBuilder.toString();

            if (result.matches(regex)) {
                String[] results = new String[capturingGroups.length];
                Matcher matcher = Pattern.compile(regex).matcher(result);
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
}
