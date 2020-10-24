package at.haha007.edenclient.mods.CheshShop;

import at.haha007.edenclient.utils.MathUtils;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.math.BlockPos;

public class ChestShopEntry {
	private BlockPos pos;
	private int sellPrice = -1, buyPrice = -1;
	private int amount;
	private String owner;
	private boolean isShop = false;
	private String item;


	public ChestShopEntry(SignBlockEntity sign) {
		CompoundTag tag = new CompoundTag();
		sign.toTag(tag);
		String[] lines = new String[4];
		for (int i = 0; i < lines.length; i++) {
			lines[i] = getString(tag.getString("Text" + (i + 1)));
		}

		String player = lines[0];
		if (player.isEmpty()) return;

		int amount;
		try {
			amount = Integer.parseInt(lines[1]);
		} catch (NumberFormatException e) {
			return;
		}

		String item = lines[3];
		if (item.isEmpty()) return;

		String[] prices = lines[2].toLowerCase().replaceAll("\\s", "").split(":");
		for (String priceString : prices) {
			if (priceString.contains("b")) {
				priceString = priceString.replace("b", "");
				if (!MathUtils.isInteger(priceString)) continue;
				int price = Integer.parseInt(priceString);
				this.buyPrice = price;
			} else if (priceString.contains("s")) {
				priceString = priceString.replace("s", "");
				if (!MathUtils.isInteger(priceString)) continue;
				int price = Integer.parseInt(priceString);
//				MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(new LiteralText(String.format("%s buys %d %s for %d", player, amount, item, price)));
				this.sellPrice = price;
			} else {
				return;
			}
		}
		pos = sign.getPos();
		this.amount = amount;
		this.isShop = true;
		this.owner = player;
		this.item = item;
	}

	public boolean isShop() {
		return isShop;
	}

	public boolean canSell() {
		return sellPrice >= 0;
	}

	public boolean canBuy() {
		return buyPrice >= 0;
	}

	public int getBuyPrice() {
		return buyPrice;
	}

	public float getBuyPricePerItem() {
		return ((float) buyPrice) / amount;
	}

	public float getSellPricePerItem() {
		return ((float) sellPrice) / amount;
	}

	public int getSellPrice() {
		return sellPrice;
	}

	public String getItem() {
		return item;
	}

	public String getOwner() {
		return owner;
	}

	public BlockPos getPos() {
		return pos;
	}

	private String getString(String string) {
		string = string.
			replaceFirst("\\{", "").
			replaceFirst("\\{", "").
			replaceFirst("\"text\":\"", "").
			replaceFirst("\"text\":\"", "").
			replaceFirst("\"}],", "").
			replaceFirst("\"extra\":\\[", "");
		return string.substring(0, string.length() - 2);
	}

	public String toString() {
		if (!isShop) return String.format("[%s] Not a Shop", pos.toString());
		StringBuilder sb = new StringBuilder();
		sb.append(String.format("pos:[%d, %d, %d]", pos.getX(), pos.getY(), pos.getZ()));
		sb.append(String.format(", owner:%s", owner));
		sb.append(String.format(", item:%s", item));
		sb.append(String.format(", amount:%d", amount));
		if (canBuy()) {
			sb.append(String.format(", buy:%d$, bpi:%3f1$/i", buyPrice, getBuyPricePerItem()));
		}
		if (canSell()) {
			sb.append(String.format(", sell:%d$, spi:%3f1$/i", sellPrice, getSellPricePerItem()));
		}
		return sb.toString();
	}
}
